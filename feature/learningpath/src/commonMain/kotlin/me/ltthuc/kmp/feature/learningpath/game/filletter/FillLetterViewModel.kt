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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioRef
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
 * Drives FillLetter — show picture + word with one missing letter (e.g. `_at`) + 2 letter
 * circle choices. Kid taps correct letter → blank fills. 4 rounds.
 *
 * Blank position biased toward the first letter (60%) for younger kids; otherwise the
 * blank can be in the middle or at the end. Distractor letter is a random non-target.
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
        val lastWrongPick: Char? = null,
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

    fun onLetterTapped(letter: Char) {
        val state = stateFlow.value
        if (state.isResolving || state.isComplete) return
        val rounds = roundsFlow.value
        val round = rounds.getOrNull(state.currentRoundIndex) ?: return
        if (letter.equals(round.correctLetter, ignoreCase = true)) {
            triggerAdvance(state, rounds)
        } else {
            val newWrongCount = state.wrongCount + 1
            Napier.v(tag = TAG) { "Wrong FillLetter tap: $letter vs target ${round.correctLetter} (count=$newWrongCount)" }
            if (newWrongCount >= WRONG_THRESHOLD) {
                Napier.d(tag = TAG) { "Auto-reveal triggered after $WRONG_THRESHOLD wrong attempts" }
                triggerAdvance(state, rounds)
            } else {
                stateFlow.value = state.copy(lastWrongPick = letter, wrongCount = newWrongCount)
            }
        }
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

    private fun buildRounds(lessons: List<PhonicsLesson>): ImmutableList<FillLetterRound> {
        // Keep each word paired with its originating lesson so we can resolve the word audio ref.
        val pool = lessons.flatMap { lesson ->
            lesson.words
                .filter { it.word.length >= MIN_WORD_LEN && !it.emoji.isNullOrBlank() }
                .map { lesson to it }
        }
        if (pool.size < 2) return persistentListOf()
        // Unit letters = distractor source (kid sticks to letters they're learning, no random alphabet noise).
        val unitLetters = lessons.map { it.letter.lowercase().first() }.distinct()
        val targets = pool.shuffled(Random.Default).take(ROUND_COUNT)
        return targets.mapIndexed { idx, (lesson, target) ->
            val word = target.word.lowercase()
            // Blank always the first letter (e.g. "_pple", "_at") — matches Fonics Game 4 design.
            val blankIndex = 0
            val correctLetter = word[blankIndex]
            // 3 distractors from the unit's other letters; pad with random alphabet only if the
            // unit has < 4 letters total (rare edge case for tiny units).
            val unitDistractorPool = unitLetters.filter { it != correctLetter }
            val distractors = if (unitDistractorPool.size >= CHOICE_COUNT - 1) {
                unitDistractorPool.shuffled(Random.Default).take(CHOICE_COUNT - 1)
            } else {
                val needed = CHOICE_COUNT - 1 - unitDistractorPool.size
                val padding = ('a'..'z')
                    .filter { it != correctLetter && it !in unitDistractorPool }
                    .shuffled(Random.Default)
                    .take(needed)
                unitDistractorPool + padding
            }
            val tint = BUBBLE_TINT_PALETTE[idx % BUBBLE_TINT_PALETTE.size]
            FillLetterRound(
                fullWord = word,
                emoji = target.emoji.orEmpty(),
                blankIndex = blankIndex,
                correctLetter = correctLetter,
                choices = (listOf(correctLetter) + distractors)
                    .shuffled(Random.Default).toImmutableList(),
                tint = tint,
                // Resolve with original-case word so wordRef's exact match succeeds.
                wordRef = lesson.wordRef(target.word),
            )
        }.toImmutableList()
    }

    /** Games swap in place, so leaving one must not leave its audio talking over the next. */
    fun onLeaveScreen() {
        audio.stop()
    }

    private companion object {
        const val TAG = "FillLetterViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val AUDIO_MAX_MS = 6_000L
        const val ROUND_COUNT = 4
        const val MIN_WORD_LEN = 3
        const val WRONG_THRESHOLD = 5
        const val CHOICE_COUNT = 4
    }
}

@Immutable
internal data class FillLetterRound(
    val fullWord: String,
    val emoji: String,
    val blankIndex: Int,
    val correctLetter: Char,
    val choices: ImmutableList<Char>,
    val tint: Color,
    val wordRef: AudioRef.Word?,
) {
    fun displayWord(filled: Boolean): String = if (filled) {
        fullWord
    } else {
        fullWord.mapIndexed { i, c -> if (i == blankIndex) '_' else c }.joinToString("")
    }
}

@Immutable
internal data class FillLetterUiState(
    val rounds: ImmutableList<FillLetterRound>,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val lastWrongPick: Char?,
    val isResolving: Boolean,
    val isComplete: Boolean,
) {
    val currentRound: FillLetterRound get() = rounds[currentRoundIndex]
}
