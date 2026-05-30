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
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.BUBBLE_TINT_PALETTE
import kotlin.random.Random

/**
 * Drives PickWord — show 1 picture, 2 word choices, kid taps the correct word matching
 * the picture. 4 rounds per game; each round draws a random target from the unit's pool
 * of words-with-emoji and pairs it with a distractor (different word from the same pool).
 *
 * Forgiving: wrong taps don't penalize, just trigger a visual shake (handled in Screen).
 */
internal class PickWordViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val sfxController: SfxController,
) : ViewModel() {

    private data class InternalState(
        val currentRoundIndex: Int = 0,
        val lastWrongPick: String? = null,
        val isResolving: Boolean = false,
        val isComplete: Boolean = false,
        val wrongCount: Int = 0,
    )

    private val roundsFlow = MutableStateFlow<ImmutableList<PickWordRound>>(persistentListOf())
    private val stateFlow = MutableStateFlow(InternalState())

    private var lastUnitIdLoaded: String? = null

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
            delay(CORRECT_HOLD_MS)
            val next = state.currentRoundIndex + 1
            if (next >= rounds.size) {
                sfxController.playVoicePraise(COMPLETE_PRAISE_POOL.random())
                stateFlow.value = stateFlow.value.copy(isComplete = true, isResolving = false)
            } else {
                stateFlow.value = stateFlow.value.copy(
                    currentRoundIndex = next,
                    isResolving = false,
                )
            }
        }
    }

    private fun buildRounds(lessons: List<PhonicsLesson>): ImmutableList<PickWordRound> {
        val pool = lessons.flatMap { lesson ->
            lesson.words.filter { !it.emoji.isNullOrBlank() }
        }
        if (pool.size < 2) return persistentListOf()
        val targets = pool.shuffled(Random.Default).take(ROUND_COUNT)
        return targets.mapIndexed { idx, target ->
            val distractor = pool.filter { it.word != target.word }.random(Random.Default)
            val tint = BUBBLE_TINT_PALETTE[idx % BUBBLE_TINT_PALETTE.size]
            PickWordRound(
                targetWord = target.word,
                targetEmoji = target.emoji.orEmpty(),
                choices = listOf(target.word, distractor.word).shuffled(Random.Default).toImmutableList(),
                tint = tint,
            )
        }.toImmutableList()
    }

    private companion object {
        const val TAG = "PickWordViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val CORRECT_HOLD_MS = 600L
        const val ROUND_COUNT = 4
        const val WRONG_THRESHOLD = 5
        val COMPLETE_PRAISE_POOL = listOf("praise_great_job", "praise_well_done", "praise_you_got_it")
    }
}

@Immutable
internal data class PickWordRound(
    val targetWord: String,
    val targetEmoji: String,
    val choices: ImmutableList<String>,
    val tint: Color,
)

@Immutable
internal data class PickWordUiState(
    val rounds: ImmutableList<PickWordRound>,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val lastWrongPick: String?,
    val isResolving: Boolean,
    val isComplete: Boolean,
) {
    val currentRound: PickWordRound get() = rounds[currentRoundIndex]
}
