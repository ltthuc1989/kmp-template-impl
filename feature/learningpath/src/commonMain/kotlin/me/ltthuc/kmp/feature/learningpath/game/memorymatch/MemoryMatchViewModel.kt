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

    // This screen's claim on the single playback channel: what it starts, only it can stop. Keeps the
    // outgoing screen's stop (which runs mid nav-transition) from cutting the incoming screen's audio.
    private val audio = AudioSession(audioRepository)

    private val lessonsFlow = MutableStateFlow<List<PhonicsLesson>>(emptyList())
    private val cardsFlow = MutableStateFlow<ImmutableList<MemoryCardSpec>>(persistentListOf())
    private val selectedIds = MutableStateFlow<ImmutableList<Int>>(persistentListOf())
    private val matchedIds = MutableStateFlow<Set<Int>>(persistentSetOf())
    private val isResolving = MutableStateFlow(false)

    /** 0 = ghép vần/chữ cái, 1 = ghép từ với hình. Hết vòng 0 thì tự dựng bàn vòng 1. */
    private val roundIndex = MutableStateFlow(0)

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
                    val fresh = buildCardsFor(lessons.toImmutableList(), roundIndex.value)
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
                            roundIndex = roundIndex.value,
                            totalRounds = TOTAL_ROUNDS,
                        ),
                    )
                } else {
                    // Hết bàn ở vòng cuối mới là xong game; hết bàn vòng 0 thì sang vòng 1.
                    val boardCleared = matched.size == cards.size && cards.isNotEmpty()
                    val isComplete = boardCleared && roundIndex.value >= TOTAL_ROUNDS - 1
                    if (boardCleared && roundIndex.value < TOTAL_ROUNDS - 1) advanceRound(lessons)
                    ScreenState.Idle(
                        MemoryMatchUiState(
                            cards = cards,
                            selectedIds = selected,
                            matchedIds = matched.toImmutableSet(),
                            isResolving = resolving,
                            isComplete = isComplete,
                            totalPairs = cards.size / 2,
                            roundIndex = roundIndex.value,
                            totalRounds = TOTAL_ROUNDS,
                        ),
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private var advancingRound = false

    /**
     * Sang vòng sau: giữ bàn cũ hiện nguyên trong [ROUND_HOLD_MS] để bé nhìn thành quả,
     * rồi mới thay bàn. Không có nhịp nghỉ này thì bàn đổi ngay lúc cặp cuối vừa lật,
     * bé không kịp thấy mình vừa ghép đúng.
     */
    private fun advanceRound(lessons: List<PhonicsLesson>) {
        if (advancingRound) return
        advancingRound = true
        viewModelScope.launch {
            delay(ROUND_HOLD_MS)
            roundIndex.value = roundIndex.value + 1
            matchedIds.value = persistentSetOf()
            selectedIds.value = persistentListOf()
            cardsFlow.value = buildCardsFor(lessons.toImmutableList(), roundIndex.value)
            advancingRound = false
        }
    }

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
        playCardSound(card)

        if (nextSelected.size == 2) {
            resolvePair(nextSelected)
        }
    }

    private fun playCardSound(card: MemoryCardSpec) {
        // Thẻ vòng 2 mang sẵn ref của TỪ (thẻ chữ lẫn thẻ hình đều phát tiếng từ đó).
        card.audio?.let {
            audio.play(it)
            return
        }
        playLetterSound(card.letter)
    }

    private fun playLetterSound(letter: String) {
        // Short letter phoneme from files/audio/phonemes/<letter>.mp3 (not the long sound-intro).
        // Vần hai ký tự không có file trong `phonemes/` (chỉ a-z) — xem BubblePopViewModel.
        val ref = if (letter.length > 1) AudioRef.Rime(letter) else AudioRef.LetterSound(letter)
        audio.play(ref)
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
                selectedIds.value = persistentListOf()
                isResolving.value = false
            } else {
                delay(MISMATCH_RESOLUTION_MS)
                selectedIds.value = persistentListOf()
                isResolving.value = false
            }
        }
    }

    private fun buildCardsFor(
        lessons: ImmutableList<PhonicsLesson>,
        round: Int,
    ): ImmutableList<MemoryCardSpec> =
        if (round == 0) buildSoundPairs(lessons) else buildWordImagePairs(lessons)

    /**
     * Vòng 1 — ghép ÂM với ÂM.
     *
     * Level 1 ghép HOA với thường của cùng chữ cái ("A" ↔ "a") — đó là thứ cần học ở cấp
     * đó. Level 2+ dạy VẦN, mà vần chỉ viết thường nên không có cặp hoa/thường; hai thẻ
     * cùng hiện đúng vần đó.
     *
     * Phải lấy `displayLetter`, KHÔNG phải `letter`: ở Level 2 `letter` là mã dữ liệu
     * "SHORT-A-AM" và chính nó từng hiện lên mặt thẻ (user báo 2026-08-13).
     */
    private fun buildSoundPairs(lessons: ImmutableList<PhonicsLesson>): ImmutableList<MemoryCardSpec> {
        val rimeMode = lessons.firstOrNull()?.isRimeLesson() == true
        val keys = if (rimeMode) {
            lessons.flatMap { it.displayLetter.trim().lowercase().split(' ') }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(MAX_PAIRS)
        } else {
            lessons.map { it.letter.uppercase() }.distinct().take(MAX_PAIRS)
        }
        if (keys.size < MIN_PAIRS) {
            Napier.w(tag = TAG) { "Unit $unitId has only ${keys.size} keys — minimum $MIN_PAIRS needed" }
        }
        var nextId = 0
        val cards = mutableListOf<MemoryCardSpec>()
        keys.forEachIndexed { pairIdx, key ->
            val tint = BUBBLE_TINT_PALETTE[pairIdx % BUBBLE_TINT_PALETTE.size]
            cards += MemoryCardSpec(id = nextId++, letter = key, pairKey = key, tint = tint)
            cards += MemoryCardSpec(
                id = nextId++,
                letter = if (rimeMode) key else key.lowercase(),
                pairKey = key,
                tint = tint,
            )
        }
        return cards.shuffled(Random.Default).toImmutableList()
    }

    /**
     * Vòng 2 — ghép TỪ với HÌNH: một thẻ chữ ("ram"), một thẻ hình (emoji của từ đó).
     *
     * Chỉ lấy từ nào có emoji; từ dùng ảnh WebP chưa hiện được trên thẻ nên bỏ qua, thà
     * ít cặp còn hơn thẻ trống. Hết từ có emoji thì rơi về ghép âm để game không kẹt.
     */
    private fun buildWordImagePairs(lessons: ImmutableList<PhonicsLesson>): ImmutableList<MemoryCardSpec> {
        val picks = lessons
            .flatMap { lesson -> lesson.words.map { lesson to it } }
            .filter { (_, word) -> !word.emoji.isNullOrBlank() }
            .shuffled(Random.Default)
            .distinctBy { (_, word) -> word.word.lowercase() }
            .take(MAX_PAIRS)
        if (picks.size < MIN_PAIRS) {
            Napier.w(tag = TAG) { "Unit $unitId thiếu từ có emoji — quay lại vòng ghép âm" }
            return buildSoundPairs(lessons)
        }
        var nextId = 0
        val cards = mutableListOf<MemoryCardSpec>()
        picks.forEachIndexed { pairIdx, (lesson, word) ->
            val tint = BUBBLE_TINT_PALETTE[pairIdx % BUBBLE_TINT_PALETTE.size]
            val text = word.word.lowercase()
            val ref = lesson.wordRef(word.word)
            cards += MemoryCardSpec(id = nextId++, letter = text, pairKey = text, tint = tint, audio = ref)
            cards += MemoryCardSpec(
                id = nextId++,
                letter = word.emoji.orEmpty(),
                pairKey = text,
                tint = tint,
                audio = ref,
            )
        }
        return cards.shuffled(Random.Default).toImmutableList()
    }

    /** Level 2+ dạy vần; nhận biết qua số cấp độ trong lesson id, không qua hình dạng `letter`. */
    private fun PhonicsLesson.isRimeLesson(): Boolean =
        (LESSON_LEVEL_REGEX.find(id)?.groupValues?.get(1)?.toIntOrNull() ?: 1) >= FIRST_RIME_LEVEL

    /** Games swap in place, so leaving one must not leave its audio talking over the next. */
    fun onLeaveScreen() {
        audio.stop()
    }

    private companion object {
        const val TAG = "MemoryMatchViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val REVEAL_HOLD_MS = 400L
        const val MISMATCH_RESOLUTION_MS = 700L
        const val MAX_PAIRS = 3
        const val MIN_PAIRS = 2
        const val TOTAL_ROUNDS = 2
        const val ROUND_HOLD_MS = 900L
        const val FIRST_RIME_LEVEL = 2
        val LESSON_LEVEL_REGEX = Regex("""^L(\d+)U""")
    }
}

@Immutable
internal data class MemoryCardSpec(
    val id: Int,
    /** Nội dung mặt thẻ: vần, chữ cái, từ, hoặc emoji. */
    val letter: String,
    val pairKey: String,
    val tint: Color,
    /** Tiếng phát khi lật. Null thì suy từ [letter] — xem playLetterSound. */
    val audio: AudioRef? = null,
)

@Immutable
internal data class MemoryMatchUiState(
    val cards: ImmutableList<MemoryCardSpec>,
    val selectedIds: ImmutableList<Int>,
    val matchedIds: kotlinx.collections.immutable.ImmutableSet<Int>,
    val isResolving: Boolean,
    val isComplete: Boolean,
    val roundIndex: Int = 0,
    val totalRounds: Int = 1,
    val totalPairs: Int,
) {
    val matchedPairs: Int get() = matchedIds.size / 2
    fun isFaceUp(id: Int): Boolean = id in matchedIds || id in selectedIds
}
