package me.ltthuc.kmp.feature.learningpath.game.memorymatch

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.BUBBLE_TINT_PALETTE
import me.ltthuc.kmp.feature.learningpath.step.common.soundIntroRef
import kotlin.random.Random

/**
 * Drives the Memory Match game — the second game in the post-Story flow.
 *
 * Pairs are uppercase+lowercase of each unit letter (Aa, Bb, Cc for L1U1). Shuffled deterministically
 * per session: a fresh enter = fresh shuffle. Tap two cards; if pairKey matches, both stay face-up.
 * Otherwise both flip back after [MISMATCH_RESOLUTION_MS] so the child has time to register both.
 *
 * No penalty on mismatch — forgiving UX per project memory.
 */
internal class MemoryMatchViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val sfxController: SfxController,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    private val lessonsFlow = MutableStateFlow<List<PhonicsLesson>>(emptyList())
    private val cardsFlow = MutableStateFlow<ImmutableList<MemoryCardSpec>>(persistentListOf())
    private val selectedIds = MutableStateFlow<ImmutableList<Int>>(persistentListOf())
    private val matchedIds = MutableStateFlow<Set<Int>>(persistentSetOf())
    private val isResolving = MutableStateFlow(false)

    private var lastUnitIdLoaded: String? = null

    val screenState: StateFlow<ScreenState<MemoryMatchUiState>> =
        combine(
            unitRepository.observeLessons(unitId).onEach { lessonsFlow.value = it },
            cardsFlow,
            selectedIds,
            matchedIds,
            isResolving,
        ) { lessons, cards, selected, matched, resolving ->
            if (lessons.isEmpty()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                if (lastUnitIdLoaded != unitId || cards.isEmpty()) {
                    val fresh = buildCardsFor(lessons.toImmutableList())
                    cardsFlow.value = fresh
                    lastUnitIdLoaded = unitId
                    ScreenState.Idle(
                        MemoryMatchUiState(
                            cards = fresh,
                            selectedIds = selected,
                            matchedIds = matched.toImmutableSet(),
                            isResolving = resolving,
                            isComplete = false,
                            totalPairs = fresh.size / 2,
                        ),
                    )
                } else {
                    val isComplete = matched.size == cards.size && cards.isNotEmpty()
                    ScreenState.Idle(
                        MemoryMatchUiState(
                            cards = cards,
                            selectedIds = selected,
                            matchedIds = matched.toImmutableSet(),
                            isResolving = resolving,
                            isComplete = isComplete,
                            totalPairs = cards.size / 2,
                        ),
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    fun onCardTapped(cardId: Int) {
        if (isResolving.value) return
        if (cardId in matchedIds.value) return
        if (cardId in selectedIds.value) return
        if (selectedIds.value.size >= 2) return

        val card = cardsFlow.value.firstOrNull { it.id == cardId } ?: return
        val nextSelected = (selectedIds.value + cardId).toImmutableList()
        selectedIds.value = nextSelected
        // Replace UI "click" chime with the card's letter phoneme — same pattern as
        // BubblePop. Lets the kid hear the letter sound while exploring cards.
        playLetterSound(card.letter)

        if (nextSelected.size == 2) {
            resolvePair(nextSelected)
        }
    }

    private fun playLetterSound(letter: String) {
        val lesson = lessonsFlow.value.firstOrNull {
            it.letter.equals(letter, ignoreCase = true)
        } ?: run {
            Napier.v(tag = TAG) { "No lesson for letter '$letter' — skipping audio" }
            return
        }
        val ref = lesson.soundIntroRef() ?: run {
            Napier.w(tag = TAG) { "No SoundIntro ref for lesson ${lesson.id}" }
            return
        }
        audioRepository.play(ref)
    }

    private fun resolvePair(pair: ImmutableList<Int>) {
        isResolving.value = true
        viewModelScope.launch {
            // Wait for the flip animation to play out so the child sees both letters fully.
            delay(REVEAL_HOLD_MS)
            val cards = cardsFlow.value
            val a = cards.firstOrNull { it.id == pair[0] }
            val b = cards.firstOrNull { it.id == pair[1] }
            if (a != null && b != null && a.pairKey == b.pairKey) {
                matchedIds.value = matchedIds.value + a.id + b.id
                sfxController.playSfx("correct")
                if (matchedIds.value.size == cards.size) {
                    sfxController.playVoicePraise(COMPLETE_PRAISE_POOL.random())
                }
                selectedIds.value = persistentListOf()
                isResolving.value = false
            } else {
                delay(MISMATCH_RESOLUTION_MS)
                selectedIds.value = persistentListOf()
                isResolving.value = false
            }
        }
    }

    private fun buildCardsFor(lessons: ImmutableList<PhonicsLesson>): ImmutableList<MemoryCardSpec> {
        val letters = lessons.map { it.letter.uppercase() }.distinct().take(MAX_PAIRS)
        if (letters.size < MIN_PAIRS) {
            Napier.w(tag = TAG) { "Unit $unitId has only ${letters.size} letters — minimum $MIN_PAIRS needed" }
        }
        var nextId = 0
        val cards = mutableListOf<MemoryCardSpec>()
        letters.forEachIndexed { pairIdx, letter ->
            val tint = BUBBLE_TINT_PALETTE[pairIdx % BUBBLE_TINT_PALETTE.size]
            cards += MemoryCardSpec(id = nextId++, letter = letter, pairKey = letter, tint = tint)
            cards += MemoryCardSpec(
                id = nextId++,
                letter = letter.lowercase(),
                pairKey = letter,
                tint = tint,
            )
        }
        return cards.shuffled(Random.Default).toImmutableList()
    }

    private companion object {
        const val TAG = "MemoryMatchViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val REVEAL_HOLD_MS = 400L
        const val MISMATCH_RESOLUTION_MS = 700L
        const val MAX_PAIRS = 3
        const val MIN_PAIRS = 2
        val COMPLETE_PRAISE_POOL = listOf("praise_great_job", "praise_well_done", "praise_you_got_it")
    }
}

@Immutable
internal data class MemoryCardSpec(
    val id: Int,
    val letter: String,
    val pairKey: String,
    val tint: Color,
)

@Immutable
internal data class MemoryMatchUiState(
    val cards: ImmutableList<MemoryCardSpec>,
    val selectedIds: ImmutableList<Int>,
    val matchedIds: kotlinx.collections.immutable.ImmutableSet<Int>,
    val isResolving: Boolean,
    val isComplete: Boolean,
    val totalPairs: Int,
) {
    val matchedPairs: Int get() = matchedIds.size / 2
    fun isFaceUp(id: Int): Boolean = id in matchedIds || id in selectedIds
}
