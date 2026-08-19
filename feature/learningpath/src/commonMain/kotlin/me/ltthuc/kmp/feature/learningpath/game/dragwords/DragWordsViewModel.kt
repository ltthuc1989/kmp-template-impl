package me.ltthuc.kmp.feature.learningpath.game.dragwords

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
 * DragWords — 4 pictures + 4 word tiles. Kid drags a word over a picture and drops.
 * If picture id == word id (we pair by index), it's a correct match: tile snaps into the
 * slot beneath the picture and locks. Wrong drops: tile springs back to origin, picture
 * shakes briefly so the kid sees their attempt registered.
 *
 * 1 round only — drag all 4 words; complete when all 4 are matched.
 */
internal class DragWordsViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val sfxController: SfxController,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    // This screen's claim on the single playback channel: what it starts, only it can stop. Keeps the
    // outgoing screen's stop (which runs mid nav-transition) from cutting the incoming screen's audio.
    private val audio = AudioSession(audioRepository)

    private data class InternalState(
        val matchedWordIndices: ImmutableSet<Int> = persistentSetOf(),
        val wrongAttemptKey: Int = 0,
        val isComplete: Boolean = false,
        val wrongCount: Int = 0,
    )

    private val itemsFlow = MutableStateFlow<ImmutableList<DragWordsItem>>(persistentListOf())
    private val stateFlow = MutableStateFlow(InternalState())

    private var lastUnitIdLoaded: String? = null

    val screenState: StateFlow<ScreenState<DragWordsUiState>> =
        combine(
            unitRepository.observeLessons(unitId),
            itemsFlow,
            stateFlow,
        ) { lessons, existingItems, state ->
            if (lessons.isEmpty()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                val items = if (lastUnitIdLoaded != unitId || existingItems.isEmpty()) {
                    val fresh = buildItems(lessons)
                    itemsFlow.value = fresh
                    lastUnitIdLoaded = unitId
                    fresh
                } else {
                    existingItems
                }
                if (items.size < 2) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(
                        DragWordsUiState(
                            items = items,
                            matchedWordIndices = state.matchedWordIndices,
                            wrongAttemptKey = state.wrongAttemptKey,
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
     * Called by the Screen when a tile is released over a picture (or near one).
     *
     * @param wordId id of the dragged tile
     * @param pictureId id of the picture the tile was dropped on; null = dropped in empty space
     * @return true if the drop was a correct match (Screen uses this to snap into slot vs spring back)
     */
    fun onWordDroppedOnPicture(wordId: Int, pictureId: Int?): Boolean {
        val state = stateFlow.value
        if (state.isComplete) return false
        if (wordId in state.matchedWordIndices) return false
        if (pictureId == null || pictureId in state.matchedWordIndices) {
            return false // dropped on empty or already-filled slot → snap back
        }

        return if (wordId == pictureId) {
            sfxController.playSfx("correct")
            val newMatched = (state.matchedWordIndices + wordId).toImmutableSet()
            val items = itemsFlow.value
            val droppedRef = items.firstOrNull { it.id == wordId }?.wordRef
            if (newMatched.size >= items.size) {
                // Final match: play this word then auto-advance to next game (no overlay/praise).
                completeAfterWord(droppedRef)
            } else {
                // Intermediate match: play the dropped word's audio (no completion gating).
                viewModelScope.launch { playWordAndAwait(droppedRef) }
            }
            stateFlow.value = state.copy(matchedWordIndices = newMatched, wrongCount = 0)
            true
        } else {
            val newWrongCount = state.wrongCount + 1
            Napier.v(tag = TAG) { "Wrong drop: word $wordId → picture $pictureId (count=$newWrongCount)" }
            if (newWrongCount >= WRONG_THRESHOLD) {
                Napier.d(tag = TAG) { "Auto-match triggered after $WRONG_THRESHOLD wrong drops" }
                val items = itemsFlow.value
                val allMatched = (0 until items.size).toSet().toImmutableSet()
                // Auto-fill the board, then play the last item's word and auto-advance.
                completeAfterWord(items.lastOrNull()?.wordRef)
                stateFlow.value = state.copy(matchedWordIndices = allMatched, wrongCount = 0)
            } else {
                stateFlow.value = state.copy(
                    wrongAttemptKey = state.wrongAttemptKey + 1,
                    wrongCount = newWrongCount,
                )
            }
            false
        }
    }

    /** Play [ref] to completion (or timeout), then mark the game complete to auto-advance. */
    private fun completeAfterWord(ref: AudioRef.Word?) {
        viewModelScope.launch {
            playWordAndAwait(ref)
            stateFlow.value = stateFlow.value.copy(isComplete = true)
        }
    }

    private suspend fun playWordAndAwait(ref: AudioRef.Word?) {
        if (ref == null) return
        audio.playAndAwait(ref, AUDIO_MAX_MS)
    }

    private fun buildItems(lessons: List<PhonicsLesson>): ImmutableList<DragWordsItem> {
        // Keep each word paired with its originating lesson so we can resolve the word audio ref.
        val perLesson = lessons.mapNotNull { lesson ->
            lesson.words.firstOrNull { !it.emoji.isNullOrBlank() }?.let { lesson to it }
        }
        val extras = lessons.flatMap { lesson ->
            lesson.words.filter { !it.emoji.isNullOrBlank() }.map { lesson to it }
        }.shuffled(Random.Default)
        val seen = perLesson.map { it.second.word }.toMutableSet()
        val selected = perLesson.toMutableList()
        for (extra in extras) {
            if (selected.size >= ITEM_COUNT) break
            if (extra.second.word !in seen) {
                selected += extra
                seen += extra.second.word
            }
        }
        if (selected.size < 2) return persistentListOf()
        val shuffled = selected.shuffled(Random.Default).take(ITEM_COUNT)
        return shuffled.mapIndexed { i, (lesson, w) ->
            val tint = BUBBLE_TINT_PALETTE[i % BUBBLE_TINT_PALETTE.size]
            DragWordsItem(
                id = i,
                word = w.word,
                emoji = w.emoji.orEmpty(),
                tint = tint,
                wordRef = lesson.wordRef(w.word),
            )
        }.toImmutableList()
    }

    /** Games swap in place, so leaving one must not leave its audio talking over the next. */
    fun onLeaveScreen() {
        audio.stop()
    }

    private companion object {
        const val TAG = "DragWordsViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val AUDIO_MAX_MS = 6_000L
        const val ITEM_COUNT = 4
        const val WRONG_THRESHOLD = 5
    }
}

@Immutable
internal data class DragWordsItem(
    val id: Int,
    val word: String,
    val emoji: String,
    val tint: Color,
    val wordRef: AudioRef.Word?,
)

@Immutable
internal data class DragWordsUiState(
    val items: ImmutableList<DragWordsItem>,
    val matchedWordIndices: ImmutableSet<Int>,
    val wrongAttemptKey: Int,
    val isComplete: Boolean,
)
