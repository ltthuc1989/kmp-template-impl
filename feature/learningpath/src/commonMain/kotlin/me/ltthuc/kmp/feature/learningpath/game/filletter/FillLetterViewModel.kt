package me.ltthuc.kmp.feature.learningpath.game.filletter

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.UnitLessons
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.AudioSession
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.BUBBLE_TINT_PALETTE
import me.ltthuc.kmp.feature.learningpath.game.common.umbrellaPatterns
import me.ltthuc.kmp.feature.learningpath.step.common.lessonPatterns
import me.ltthuc.kmp.feature.learningpath.step.common.level
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef
import kotlin.random.Random

/**
 * Drives FillLetter — picture + word with the lesson's CHUNK blanked out (`fa_er` → th, `h_m_` →
 * o_e) + 4 chunk choices. Kid taps the right one → blank fills. 4 rounds.
 *
 * Which chunk is blanked and where the 3 distractors come from is decided in `FillChunk.kt`
 * (chốt 2026-09-17): same kind and same length group as the answer, borrowed from earlier units
 * when this unit runs short. Needs the whole curriculum for that, hence [UnitRepository.observeCurriculum].
 *
 * The word is spoken when each round starts (the screen calls [playRoundWord] once the narrator
 * is done): a distractor often spells another real word (`c_` + at = cat under a picture of a
 * cap), and hearing "cap" is what settles it.
 */
internal class FillLetterViewModel(
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
        val isComplete: Boolean = false,
        val wrongCount: Int = 0,
    )

    private val roundsFlow = MutableStateFlow<ImmutableList<FillLetterRound>>(persistentListOf())
    private val stateFlow = MutableStateFlow(InternalState())

    private var lastUnitIdLoaded: String? = null

    val screenState: StateFlow<ScreenState<FillLetterUiState>> =
        combine(
            unitRepository.observeLessons(unitId),
            unitRepository.observeCurriculum(),
            roundsFlow,
            stateFlow,
        ) { lessons, curriculum, existingRounds, state ->
            if (lessons.isEmpty()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                val rounds = if (lastUnitIdLoaded != unitId || existingRounds.isEmpty()) {
                    val fresh = buildRounds(lessons, curriculum)
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
                        FillLetterUiState(
                            rounds = rounds,
                            currentRoundIndex = state.currentRoundIndex.coerceIn(0, rounds.lastIndex),
                            totalRounds = rounds.size,
                            lastWrongPick = state.lastWrongPick,
                            isResolving = state.isResolving,
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

    fun onChoiceTapped(choice: String) {
        val state = stateFlow.value
        if (state.isResolving || state.isComplete) return
        val rounds = roundsFlow.value
        val round = rounds.getOrNull(state.currentRoundIndex) ?: return
        if (choice == round.answer) {
            triggerAdvance(state, rounds)
        } else {
            val newWrongCount = state.wrongCount + 1
            Napier.v(tag = TAG) { "Wrong FillLetter tap: $choice vs target ${round.answer} (count=$newWrongCount)" }
            if (newWrongCount >= WRONG_THRESHOLD) {
                Napier.d(tag = TAG) { "Auto-reveal triggered after $WRONG_THRESHOLD wrong attempts" }
                triggerAdvance(state, rounds)
            } else {
                stateFlow.value = state.copy(lastWrongPick = choice, wrongCount = newWrongCount)
            }
        }
    }

    /** Speaks the current round's word — on round start, and again whenever the kid taps the picture. */
    fun playRoundWord() {
        val state = stateFlow.value
        if (state.isResolving || state.isComplete) return
        val ref = roundsFlow.value.getOrNull(state.currentRoundIndex)?.wordRef ?: return
        audio.play(ref)
    }

    private fun triggerAdvance(state: InternalState, rounds: ImmutableList<FillLetterRound>) {
        sfxController.playSfx("correct")
        stateFlow.value = state.copy(lastWrongPick = null, isResolving = true, wrongCount = 0)
        viewModelScope.launch {
            // Play the just-completed word's audio and wait for it to finish before advancing.
            playWordAndAwait(rounds.getOrNull(state.currentRoundIndex)?.wordRef)
            val next = state.currentRoundIndex + 1
            if (next >= rounds.size) {
                // Final round: auto-advance to next game (no completion praise / overlay).
                stateFlow.value = stateFlow.value.copy(isComplete = true, isResolving = false)
            } else {
                stateFlow.value = stateFlow.value.copy(
                    currentRoundIndex = next,
                    isResolving = false,
                )
            }
        }
    }

    private suspend fun playWordAndAwait(ref: AudioRef.Word?) {
        if (ref == null) return
        audio.playAndAwait(ref, AUDIO_MAX_MS)
    }

    private fun buildRounds(
        lessons: List<PhonicsLesson>,
        curriculum: List<UnitLessons>,
    ): ImmutableList<FillLetterRound> {
        val umbrella = if ((lessons.first().level() ?: 1) >= FIRST_UMBRELLA_LEVEL) {
            lessons.umbrellaPatterns(lessons.flatMap { it.lessonPatterns() })
        } else {
            emptySet()
        }
        val pool = lessons.flatMap { lesson ->
            lesson.words
                // lọc theo `displays` chứ KHÔNG theo `emoji`: từ có ảnh WebP riêng mà thiếu emoji
                // thay thế sẽ bị `!emoji.isNullOrBlank()` loại oan — đúng bẫy mà KDoc của
                // `LessonWord.emoji` cảnh báo.
                .filter { it.word.length >= MIN_WORD_LEN && it.displays.isNotEmpty() }
                .mapNotNull { word -> candidateFor(lesson, word, umbrella) }
        }
            // Một từ có thể nằm ở hai bài của cùng unit (`jug` ở `u` và `ug` của L2U7) — chơi một lần thôi.
            .distinctBy { it.word.word.lowercase() }
        if (pool.size < 2) {
            Napier.w(tag = TAG) { "Unit $unitId has only ${pool.size} playable word(s) — showing error" }
            return persistentListOf()
        }

        val unitLabels = unitLabelsFor(lessons, curriculum)
        val unitIndex = curriculum.indexOfFirst { it.unit.id == unitId }.coerceAtLeast(0)
        val targets = pool.shuffled(Random.Default).take(ROUND_COUNT)
        return targets.mapIndexed { idx, candidate ->
            val chunk = candidate.chunk
            val distractors = pickDistractors(
                tiers = distractorTiers(chunk, unitIndex, unitLabels),
                count = CHOICE_COUNT - 1,
                random = Random.Default,
            )
            if (distractors.size < CHOICE_COUNT - 1) {
                Napier.e(tag = TAG) {
                    "Only ${distractors.size} distractor(s) for '${chunk.label}' (${candidate.word.word}) in $unitId"
                }
            }
            FillLetterRound(
                fullWord = candidate.word.word,
                picture = candidate.word,
                blankSpans = chunk.spans.toImmutableList(),
                answer = chunk.label,
                choices = (listOf(chunk.label) + distractors).shuffled(Random.Default).toImmutableList(),
                tint = BUBBLE_TINT_PALETTE[idx % BUBBLE_TINT_PALETTE.size],
                // Resolve with original-case word so wordRef's exact match succeeds.
                wordRef = candidate.lesson.wordRef(candidate.word.word),
            )
        }.toImmutableList()
    }

    private fun candidateFor(lesson: PhonicsLesson, word: LessonWord, umbrella: Set<String>): Candidate? =
        when (val lookup = fillChunkFor(lesson, word, umbrella)) {
            is ChunkLookup.Found -> Candidate(lesson, word, lookup.chunk)
            ChunkLookup.Umbrella -> null
            is ChunkLookup.Broken -> {
                Napier.w(tag = TAG) { "Skipping '${word.word}' in ${lesson.id}: ${lookup.reason}" }
                null
            }
        }

    /**
     * Thẻ vần của từng unit theo thứ tự học. Luồng curriculum rỗng hoặc thiếu unit này (DB chưa
     * seed xong, dữ liệu lệch) thì vẫn chơi được bằng thẻ của riêng unit — nhưng log, vì khi đó
     * unit thiếu vần sẽ ra vòng dưới 4 thẻ.
     */
    private fun unitLabelsFor(lessons: List<PhonicsLesson>, curriculum: List<UnitLessons>): List<List<ChunkLabel>> {
        if (curriculum.none { it.unit.id == unitId }) {
            Napier.w(tag = TAG) { "Curriculum (${curriculum.size} units) lacks $unitId — distractors from this unit only" }
            return listOf(lessons.flatMap { it.fillLabels() })
        }
        return curriculum.map { unit -> unit.lessons.flatMap { it.fillLabels() } }
    }

    /** Games swap in place, so leaving one must not leave its audio talking over the next. */
    fun onLeaveScreen() {
        audio.stop()
    }

    private data class Candidate(val lesson: PhonicsLesson, val word: LessonWord, val chunk: FillChunk)

    private companion object {
        const val TAG = "FillLetterViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val AUDIO_MAX_MS = 6_000L
        const val ROUND_COUNT = 4
        const val MIN_WORD_LEN = 3
        const val WRONG_THRESHOLD = 5
        const val CHOICE_COUNT = 4

        /** Cấp đầu tiên có vần bao — cùng mốc với Bubble Pop. */
        const val FIRST_UMBRELLA_LEVEL = 3
    }
}

@Immutable
internal data class FillLetterRound(
    val fullWord: String,
    val picture: LessonWord?,
    /** Khoảng ký tự bị che trong [fullWord] — hai khoảng khi đáp án là magic-e tách. */
    val blankSpans: ImmutableList<IntRange>,
    val answer: String,
    val choices: ImmutableList<String>,
    val tint: Color,
    val wordRef: AudioRef.Word?,
)

@Immutable
internal data class FillLetterUiState(
    val rounds: ImmutableList<FillLetterRound>,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val lastWrongPick: String?,
    val isResolving: Boolean,
    val isComplete: Boolean,
) {
    val currentRound: FillLetterRound get() = rounds[currentRoundIndex]
}
