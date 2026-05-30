package me.ltthuc.kmp.feature.learningpath.game.spellletters

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
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
 * Drives SpellLetters — kid drags letter tiles from a scatter row to fill the target
 * word's slots. Industry-standard drag-and-drop (Endless Alphabet, Khan Kids, Fonics):
 * wrong drop → tile springs back to scatter (built-in safety net), correct drop → tile
 * locks into slot. No explicit undo: if the kid drops correctly, they got it right and
 * we move on; if wrong, the spring-back lets them try again.
 *
 * Constraints to keep gameplay clean:
 * - Words must have **unique letters** (so each tile maps unambiguously to one slot).
 * - Word length 3-4 (kid 3-8 can manage; longer is overwhelming).
 *
 * 3 rounds; each round shuffles tile order.
 */
internal class SpellLettersViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val sfxController: SfxController,
) : ViewModel() {

    private data class InternalState(
        val currentRoundIndex: Int = 0,
        val filledSlots: ImmutableSet<Int> = persistentSetOf(),
        val isComplete: Boolean = false,
        val wrongCount: Int = 0,
    )

    private val roundsFlow = MutableStateFlow<ImmutableList<SpellLettersRound>>(persistentListOf())
    private val stateFlow = MutableStateFlow(InternalState())

    private var lastUnitIdLoaded: String? = null

    val screenState: StateFlow<ScreenState<SpellLettersUiState>> =
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
                        SpellLettersUiState(
                            rounds = rounds,
                            currentRoundIndex = state.currentRoundIndex.coerceIn(0, rounds.lastIndex),
                            totalRounds = rounds.size,
                            filledSlots = state.filledSlots,
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

    /**
     * Drop callback fired by [me.ltthuc.kmp.feature.learningpath.game.spellletters.view.DraggableLetterTile].
     * @return true if [letter] correctly matches `round.word[slotIndex]`; false otherwise
     * (caller will spring tile back to origin).
     */
    fun onLetterDroppedOnSlot(letter: Char, slotIndex: Int?): Boolean {
        val state = stateFlow.value
        if (state.isComplete) return false
        val rounds = roundsFlow.value
        val round = rounds.getOrNull(state.currentRoundIndex) ?: return false
        val isMatch = slotIndex != null &&
            slotIndex in round.word.indices &&
            slotIndex !in state.filledSlots &&
            round.word[slotIndex].equals(letter, ignoreCase = true)

        if (isMatch && slotIndex != null) {
            sfxController.playSfx("correct")
            val newFilled = (state.filledSlots + slotIndex).toImmutableSet()
            advanceFilledState(state.copy(wrongCount = 0), newFilled, round, rounds)
            return true
        }

        // Don't count drops on already-filled slots as wrong (kid just missed an empty one).
        if (slotIndex != null && slotIndex in state.filledSlots) return false

        val newWrongCount = state.wrongCount + 1
        Napier.v(tag = TAG) { "Wrong SpellLetters drop: $letter on $slotIndex (count=$newWrongCount)" }
        if (newWrongCount >= WRONG_THRESHOLD) {
            Napier.d(tag = TAG) { "Auto-fill triggered after $WRONG_THRESHOLD wrong drops" }
            val allSlots = round.word.indices.toSet().toImmutableSet()
            advanceFilledState(state.copy(wrongCount = 0), allSlots, round, rounds)
        } else {
            stateFlow.value = state.copy(wrongCount = newWrongCount)
        }
        return false
    }

    private fun advanceFilledState(
        baseState: InternalState,
        newFilled: ImmutableSet<Int>,
        round: SpellLettersRound,
        rounds: ImmutableList<SpellLettersRound>,
    ) {
        if (newFilled.size >= round.word.length) {
            stateFlow.value = baseState.copy(filledSlots = newFilled)
            viewModelScope.launch {
                delay(ROUND_HOLD_MS)
                val next = baseState.currentRoundIndex + 1
                if (next >= rounds.size) {
                    sfxController.playVoicePraise(COMPLETE_PRAISE_POOL.random())
                    stateFlow.value = stateFlow.value.copy(isComplete = true)
                } else {
                    stateFlow.value = stateFlow.value.copy(
                        currentRoundIndex = next,
                        filledSlots = persistentSetOf(),
                        wrongCount = 0,
                    )
                }
            }
        } else {
            stateFlow.value = baseState.copy(filledSlots = newFilled)
        }
    }

    private fun buildRounds(lessons: List<PhonicsLesson>): ImmutableList<SpellLettersRound> {
        val pool = lessons.flatMap { lesson ->
            lesson.words.filter {
                val w = it.word.lowercase()
                w.length in MIN_LEN..MAX_LEN && w.toSet().size == w.length // unique letters only
            }
        }
        if (pool.size < ROUND_COUNT) {
            Napier.w(tag = TAG) { "Unit $unitId only has ${pool.size} unique-letter words for SpellLetters" }
        }
        val targets = pool.shuffled(Random.Default).take(ROUND_COUNT)
        if (targets.isEmpty()) return persistentListOf()
        return targets.mapIndexed { idx, target ->
            val word = target.word.lowercase()
            val tint = BUBBLE_TINT_PALETTE[idx % BUBBLE_TINT_PALETTE.size]
            SpellLettersRound(
                word = word,
                emoji = target.emoji.orEmpty(),
                tileOrder = word.toList().shuffled(Random.Default).toImmutableList(),
                tint = tint,
            )
        }.toImmutableList()
    }

    private companion object {
        const val TAG = "SpellLettersViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val ROUND_HOLD_MS = 800L
        const val ROUND_COUNT = 3
        const val WRONG_THRESHOLD = 5
        const val MIN_LEN = 3
        const val MAX_LEN = 4
        val COMPLETE_PRAISE_POOL = listOf("praise_great_job", "praise_well_done", "praise_you_got_it")
    }
}

@Immutable
internal data class SpellLettersRound(
    val word: String,
    val emoji: String,
    val tileOrder: ImmutableList<Char>, // shuffled letters of `word`
    val tint: Color,
)

@Immutable
internal data class SpellLettersUiState(
    val rounds: ImmutableList<SpellLettersRound>,
    val currentRoundIndex: Int,
    val totalRounds: Int,
    val filledSlots: ImmutableSet<Int>,
    val isComplete: Boolean,
) {
    val currentRound: SpellLettersRound get() = rounds[currentRoundIndex]
}
