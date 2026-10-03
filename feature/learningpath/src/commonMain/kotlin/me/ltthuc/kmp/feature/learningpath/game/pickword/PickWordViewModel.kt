package me.ltthuc.kmp.feature.learningpath.game.pickword

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.AudioSession
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.BUBBLE_TINT_PALETTE
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef
import kotlin.random.Random

/**
 * Drives PickWord — show 1 picture, 2 word choices, kid taps the correct word matching
 * the picture. 4 rounds per game; each round draws a random target from the unit's pool
 * of words-with-emoji and pairs it with a distractor (different word from the same pool).
 *
 * Forgiving: wrong taps don't penalize, just trigger a visual shake (handled in Screen).
 *
 * Every tap speaks the word that was tapped (user báo 2026-10-03: "chọn word không đọc đáp án")
 * — a wrong pick too, like Fill Letter's choices, so the kid hears what they chose.
 */
internal class PickWordViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val sfxController: SfxController,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    // This screen's claim on the single playback channel: what it starts, only it can stop. Keeps the
    // outgoing screen's stop (which runs mid nav-transition) from cutting the incoming screen's audio.
    private val audio = AudioSession(audioRepository)

    private data class InternalState(
        val currentRoundIndex: Int = 0,
        val lastWrongPick: String? = null,
        val isResolving: Boolean = false,
        /** Từ đã vào ô đáp án — bật ngay khi chọn đúng, TRƯỚC tiếng của từ. */
        val isFilled: Boolean = false,
        val isComplete: Boolean = false,
        val wrongCount: Int = 0,
    )

    private val roundsFlow = MutableStateFlow<ImmutableList<PickWordRound>>(persistentListOf())
    private val stateFlow = MutableStateFlow(InternalState())

    private var lastUnitIdLoaded: String? = null

    // Tiếng của MỌI từ trong unit, để chạm thẻ sai cũng đọc được từ của thẻ đó.
    private var wordSounds: Map<String, AudioRef.Word> = emptyMap()

    val screenState: StateFlow<ScreenState<PickWordUiState>> =
        combine(
            unitRepository.observeLessons(unitId),
            roundsFlow,
            stateFlow,
        ) { lessons, existingRounds, state ->
            if (lessons.isEmpty()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                val rounds = if (lastUnitIdLoaded != unitId || existingRounds.isEmpty()) {
                    val fresh = buildRounds(lessons)
                    roundsFlow.value = fresh
                    lastUnitIdLoaded = unitId
                    fresh
                } else {
                    existingRounds
                }
                if (rounds.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(
                        PickWordUiState(
                            rounds = rounds,
                            currentRoundIndex = state.currentRoundIndex.coerceIn(0, rounds.lastIndex),
                            totalRounds = rounds.size,
                            lastWrongPick = state.lastWrongPick,
                            isResolving = state.isResolving,
                            isFilled = state.isFilled,
                            isComplete = state.isComplete,
                        ),
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    fun onWordTapped(word: String) {
        val state = stateFlow.value
        if (state.isResolving || state.isComplete) return
        val rounds = roundsFlow.value
        val round = rounds.getOrNull(state.currentRoundIndex) ?: return
        if (word == round.targetWord) {
            triggerAdvance(state, rounds)
        } else {
            soundFor(word)?.let(audio::play)
            val newWrongCount = state.wrongCount + 1
            Napier.v(tag = TAG) { "Wrong PickWord tap: $word vs target ${round.targetWord} (count=$newWrongCount)" }
            if (newWrongCount >= WRONG_THRESHOLD) {
                Napier.d(tag = TAG) { "Auto-reveal triggered after $WRONG_THRESHOLD wrong attempts" }
                triggerAdvance(state, rounds)
            } else {
                stateFlow.value = state.copy(lastWrongPick = word, wrongCount = newWrongCount)
            }
        }
    }

    private fun triggerAdvance(state: InternalState, rounds: ImmutableList<PickWordRound>) {
        sfxController.playSfx("correct")
        stateFlow.value = state.copy(lastWrongPick = null, isResolving = true, wrongCount = 0)
        viewModelScope.launch {
            // User chốt 2026-10-03: ghép từ vào ô → nghỉ 0,5s (cùng nhịp Fill Letter) → đọc từ →
            // nghỉ 1s → sang hình kế. Vòng cuối không nghỉ ở đây: màn hình tự chờ 1s.
            val round = rounds.getOrNull(state.currentRoundIndex)
            if (round?.wordRef == null) Napier.w(tag = TAG) { "No audio for '${round?.targetWord}' in $unitId" }
            stateFlow.value = stateFlow.value.copy(isFilled = true)
            delay(FILLED_TO_WORD_MS)
            playWordAndAwait(round?.wordRef)
            val next = state.currentRoundIndex + 1
            if (next >= rounds.size) {
                // Final round: auto-advance to next game (no completion praise / overlay).
                stateFlow.value = stateFlow.value.copy(isComplete = true, isResolving = false)
            } else {
                delay(NEXT_WORD_PAUSE_MS)
                stateFlow.value = stateFlow.value.copy(
                    currentRoundIndex = next,
                    isResolving = false,
                    isFilled = false,
                )
            }
        }
    }

    private fun soundFor(word: String): AudioRef.Word? {
        val ref = wordSounds[word]
        if (ref == null) Napier.w(tag = TAG) { "No sound for word '$word' in $unitId — tap stays silent" }
        return ref
    }

    private suspend fun playWordAndAwait(ref: AudioRef.Word?) {
        if (ref == null) return
        audio.playAndAwait(ref, AUDIO_MAX_MS)
    }

    private fun buildRounds(lessons: List<PhonicsLesson>): ImmutableList<PickWordRound> {
        // Keep each word paired with its originating lesson so we can resolve the word audio ref.
        val pool = lessons.flatMap { lesson ->
            // lọc theo `displays` chứ KHÔNG theo `emoji`: từ có ảnh WebP riêng mà thiếu emoji
            // thay thế sẽ bị `!emoji.isNullOrBlank()` loại oan — đúng bẫy mà KDoc của
            // `LessonWord.emoji` cảnh báo.
            lesson.words.filter { it.displays.isNotEmpty() }.map { lesson to it }
        }
        wordSounds = pool.mapNotNull { (lesson, w) -> lesson.wordRef(w.word)?.let { w.word to it } }.toMap()
        if (pool.size < 2) return persistentListOf()
        val targets = pool.shuffled(Random.Default).take(ROUND_COUNT)
        return targets.mapIndexed { idx, (lesson, target) ->
            val distractor = pool.filter { it.second.word != target.word }.random(Random.Default).second
            val tint = BUBBLE_TINT_PALETTE[idx % BUBBLE_TINT_PALETTE.size]
            PickWordRound(
                targetWord = target.word,
                picture = target,
                choices = listOf(target.word, distractor.word).shuffled(Random.Default).toImmutableList(),
                tint = tint,
                wordRef = lesson.wordRef(target.word),
            )
        }.toImmutableList()
    }

    /** Games swap in place, so leaving one must not leave its audio talking over the next. */
    fun onLeaveScreen() {
        audio.stop()
    }

    private companion object {
        const val TAG = "PickWordViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val AUDIO_MAX_MS = 6_000L
        const val ROUND_COUNT = 4
        const val WRONG_THRESHOLD = 5

        /** Từ đã vào ô → nghỉ chừng này rồi mới đọc. */
        const val FILLED_TO_WORD_MS = 500L

        /** Nghỉ sau tiếng của từ rồi mới sang hình kế. */
        const val NEXT_WORD_PAUSE_MS = 1_000L
    }
}

@Immutable
internal data class PickWordRound(
    val targetWord: String,
    /** Từ để vẽ hình — ảnh WebP nếu có, không thì emoji. Xem `PicturePanel`. */
    val picture: LessonWord?,
    val choices: ImmutableList<String>,
    val tint: Color,
    val wordRef: AudioRef.Word?,
)

@Immutable
internal data class PickWordUiState(
    val rounds: ImmutableList<PickWordRound>,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val lastWrongPick: String?,
    val isResolving: Boolean,
    val isFilled: Boolean,
    val isComplete: Boolean,
) {
    val currentRound: PickWordRound get() = rounds[currentRoundIndex]
}
