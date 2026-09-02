package me.ltthuc.kmp.feature.learningpath.game.bubblepop

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.AudioSession
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.game.common.rimeAudioKeys
import me.ltthuc.kmp.feature.learningpath.game.common.wordHasPattern

/**
 * Drives BubblePop v5: 30s round, kid races to pop up to [TARGET_POOL] target letter bubbles.
 *
 * Per round:
 * - Target letter = unit's letter for `roundIndex` (typically 3 rounds = 3 letters)
 * - BubbleCanvas spawns 2 target + 4 distractor bubbles at any time, recycling as they
 *   drift off-top (existing behavior unchanged)
 * - Counter caps at [TARGET_POOL] = 10 — total target pops needed for max stars
 * - Timer counts down from [ROUND_DURATION_MS] = 30s
 * - Round ends when popCount >= 10 (early, 5⭐) OR timer reaches 0 (tier stars by score)
 *
 * Scoring tiers (per user spec v5):
 *  - 10/10 → 5⭐
 *  -  8-9 → 4⭐
 *  -  5-7 → 3⭐
 *  -  2-4 → 2⭐
 *  -  0-1 → 1⭐
 *
 * No v4 auto-reveal — timer is natural safety net.
 */
internal class BubblePopViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
    private val sfxController: SfxController,
) : ViewModel() {

    // This screen's claim on the single playback channel: what it starts, only it can stop. Keeps the
    // outgoing screen's stop (which runs mid nav-transition) from cutting the incoming screen's audio.
    private val audio = AudioSession(audioRepository)

    private val roundIndex = MutableStateFlow(0)
    private val popCount = MutableStateFlow(0)
    private val timeRemainingMs = MutableStateFlow(ROUND_DURATION_MS)
    private val roundComplete = MutableStateFlow(false)
    private val gameComplete = MutableStateFlow(false)
    private val roundStars = MutableStateFlow(0)

    /** True while the round-start guide audio plays — bubbles hidden + timer paused until it ends. */
    private val guidePlaying = MutableStateFlow(false)

    /** Vần đang được hiện ra giữa màn trước khi bong bóng nổi lên; null = không hiện. */
    private val revealedTarget = MutableStateFlow<String?>(null)

    private val bubblesCache = mutableMapOf<Int, ImmutableList<BubbleSpec>>()

    /** Nhãn vần → khoá file audio, rỗng ở cấp 1-2. Xem [rimeAudioKeys]. */
    private var audioKeys: Map<String, String> = emptyMap()

    private var timerJob: Job? = null

    /** Round index whose guide prompt has already been kicked off (fire-once-per-round guard). */
    private var lastRoundStarted = -1

    val screenState: StateFlow<ScreenState<BubblePopUiState>> =
        combine(
            unitRepository.observeLessons(unitId).onEach { audioKeys = it.rimeAudioKeys() },
            combine(roundIndex, popCount, timeRemainingMs) { r, p, t -> Triple(r, p, t) },
            combine(roundComplete, gameComplete, roundStars) { rc, gc, rs -> Triple(rc, gc, rs) },
            combine(guidePlaying, revealedTarget) { g, r -> g to r },
        ) { lessons, roundData, statusData, guideData ->
            val (round, popped, time) = roundData
            val (isRoundDone, isGameDone, stars) = statusData
            val (guide, revealed) = guideData
            if (lessons.isEmpty()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                val rimeMode = lessons.first().isRimeLesson()
                val unitLetters = lessons.gameTargets().toImmutableList()
                val totalRounds = unitLetters.size.coerceAtLeast(1)
                val clampedRound = round.coerceIn(0, totalRounds - 1)
                val target = unitLetters[clampedRound]
                val bubbles = bubblesForRound(target, unitLetters, clampedRound, rimeMode)
                ScreenState.Idle(
                    BubblePopUiState(
                        unitLetters = unitLetters,
                        roundIndex = clampedRound,
                        totalRounds = totalRounds,
                        targetLetter = target,
                        targetAudioKey = audioKeys[target] ?: target,
                        revealedTarget = revealed,
                        bubbles = bubbles,
                        popCount = popped,
                        targetPool = TARGET_POOL,
                        timeRemainingMs = time,
                        roundDurationMs = ROUND_DURATION_MS,
                        isRoundComplete = isRoundDone,
                        isGameComplete = isGameDone,
                        roundStars = stars,
                        isGuidePlaying = guide,
                        isRimeMode = rimeMode,
                    ),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    val audioState: StateFlow<AudioState> = audioRepository.state

    init {
        viewModelScope.launch {
            // On each new active round: play the "Can you find the <sound> sound?" guide prompt
            // first (bubbles hidden, timer paused), then reveal bubbles and start the timer once
            // the guide finishes. Fires once per round via lastRoundStarted.
            screenState.collect { state ->
                if (state is ScreenState.Idle) {
                    val ui = state.data
                    if (!ui.isRoundComplete && !ui.isGameComplete) {
                        startRoundIfNeeded(ui)
                    }
                }
            }
        }
    }

    private fun startRoundIfNeeded(ui: BubblePopUiState) {
        if (ui.roundIndex == lastRoundStarted || ui.targetLetter.isEmpty()) return
        lastRoundStarted = ui.roundIndex
        viewModelScope.launch {
            guidePlaying.value = true // hide bubbles, keep timer paused
            delay(ROUND_START_DELAY_MS) // brief beat before the guide speaks
            val prompt = if (ui.isRimeMode) {
                // Khoá theo [targetAudioKey] chứ không theo nhãn: chữ `y` dạy /iː/ ở unit 5
                // (candy) và /aɪ/ ở unit 6 (spy), cùng nhãn nhưng hai âm khác nhau nên phải
                // hỏi bằng hai câu khác nhau. Cùng luật đã dùng cho `rimes/`, xem [rimeAudioKeys].
                AudioRef.FindRime(ui.targetAudioKey)
            } else {
                AudioRef.FindSound(ui.targetLetter)
            }
            audio.playAndAwait(prompt, GUIDE_AUDIO_MAX_MS)
            revealTarget(ui)
            guidePlaying.value = false // bubbles appear
            ensureTimerRunning() // timer starts only now
        }
    }

    /**
     * Hiện vần mục tiêu ra giữa màn VÀ đọc nó, một lượt, trước khi bong bóng nổi lên.
     *
     * Vì sao cần: từ cấp 3 trở đi, các vần trong CÙNG một unit đọc lên giống hệt nhau —
     * `ai` với `ay` đều /eɪ/, `ee` `ea` `y` `ey` đều /iː/. Nghe không tài nào tách được;
     * đó là sự thật ngữ âm chứ không phải lỗi bản thu. Bé phải NHÌN thấy vần một lượt thì
     * vòng chơi mới giải được. (Cùng lý do mọi bài dạy `ai`/`ay` đều là word sort có nhãn
     * cột hiện sẵn, không phải bài nghe.)
     *
     * Hiện rồi ẨN chứ không để suốt vòng: để suốt thì thành dò chữ, bé không phải nhớ gì.
     * Ẩn đi thì vẫn phải giữ hình dạng vần trong đầu 30 giây.
     *
     * Nhịp: câu hỏi dứt → nghỉ [REVEAL_DELAY_MS] → thẻ hiện đúng [REVEAL_DURATION_MS] →
     * thẻ ẩn, bong bóng nổi lên. Tiếng đọc vần phát NGAY khi thẻ hiện và chạy song song
     * chứ không nối tiếp, nên thời gian thẻ nằm trên màn không đổi theo độ dài bản thu
     * (mảnh vần dài ngắn khác nhau, 322–850ms). Chỉnh nhịp thì sửa hai hằng số đó.
     *
     * Cấp 1 không cần — mỗi chữ cái một âm riêng, nghe là đủ.
     */
    private suspend fun revealTarget(ui: BubblePopUiState) {
        if (!ui.isRimeMode) return
        delay(REVEAL_DELAY_MS) // một nhịp lặng để câu hỏi và tiếng vần không dính vào nhau
        revealedTarget.value = ui.targetLetter
        audio.play(AudioRef.Rime(ui.targetAudioKey))
        delay(REVEAL_DURATION_MS)
        revealedTarget.value = null
    }

    /**
     * Tiếng phát khi chạm một bong bóng — cả bong bóng đúng lẫn bong bóng nhiễu, để bé
     * học bằng cách sờ thử.
     */
    private fun playBubbleSound(spec: BubbleSpec) {
        // Vần của bài đi trước: chỉ nó mới có khoá trong [audioKeys], và ở cấp 3 khoá đó
        // không trùng nhãn (`y` → `y_eee`). Bong bóng nhiễu là chữ cái a-z nên trượt bảng
        // và rơi xuống nhánh dưới — đúng chỗ nó cần đến.
        //
        // Nhánh dưới chọn theo ĐỘ DÀI nhãn, không theo chế độ chơi. Ở Level 2 bong bóng
        // nhiễu là chữ cái a-z, mà `rimes/` chỉ có vần hai ký tự — cho cả hai đi qua
        // `rimes/` thì chạm chữ cái là im lặng (user báo 2026-08-12). Ngược lại vần một
        // ký tự ("a") dùng `phonemes/a.mp3` cũng đúng: đó chính là âm /æ/ ngắn.
        val letter = spec.letter
        val ref = audioKeys[letter]?.let(AudioRef::Rime)
            ?: if (letter.length > 1) AudioRef.Rime(letter) else AudioRef.LetterSound(letter)
        audio.play(ref)
    }

    private fun ensureTimerRunning() {
        if (timerJob?.isActive == true) return
        timerJob = viewModelScope.launch {
            while (timeRemainingMs.value > 0 && !roundComplete.value && !gameComplete.value) {
                delay(TICK_MS)
                val newRemaining = (timeRemainingMs.value - TICK_MS).coerceAtLeast(0)
                timeRemainingMs.value = newRemaining
                if (newRemaining == 0L && !roundComplete.value) {
                    endRound()
                }
            }
        }
    }

    fun onBubbleTapped(spec: BubbleSpec, isCorrect: Boolean) {
        if (roundComplete.value || gameComplete.value) return
        // Always play the bubble's sound on any tap — kid learns by exploring.
        playBubbleSound(spec)
        if (isCorrect) {
            // Per user spec v5d: tap target → only the letter phoneme (already played above).
            // No "correct" chime — keeps audio focus on the phoneme as the reward.
            val newCount = (popCount.value + 1).coerceAtMost(TARGET_POOL)
            popCount.value = newCount
            if (newCount >= TARGET_POOL) {
                Napier.d(tag = TAG) { "Early round end — full $TARGET_POOL targets popped" }
                endRound()
            }
        } else {
            Napier.v(tag = TAG) { "Wrong bubble tapped: ${spec.letter}" }
        }
    }

    private fun endRound() {
        timerJob?.cancel()
        val stars = starsFor(popCount.value)
        roundStars.value = stars
        roundComplete.value = true
        sfxController.playVoicePraise(PRAISE_POOL.random())
    }

    /** Called by the screen when the user dismisses the round-end overlay. */
    fun onAdvanceRound() {
        val next = roundIndex.value + 1
        val total = totalRoundsSnapshot()
        if (next >= total) {
            gameComplete.value = true
            roundComplete.value = false
            sfxController.playVoicePraise(FINAL_PRAISE_POOL.random())
        } else {
            roundIndex.value = next
            popCount.value = 0
            timeRemainingMs.value = ROUND_DURATION_MS
            roundComplete.value = false
            roundStars.value = 0
            // Timer auto-restarts via the screenState init collector.
        }
    }

    fun onLeaveScreen() {
        timerJob?.cancel()
        audio.stop()
    }

    private fun totalRoundsSnapshot(): Int =
        (screenState.value as? ScreenState.Idle)?.data?.totalRounds ?: 1

    private fun bubblesForRound(
        target: String,
        unitLetters: List<String>,
        round: Int,
        rimeMode: Boolean,
    ): ImmutableList<BubbleSpec> {
        bubblesCache[round]?.let { return it }
        val fresh = if (rimeMode) {
            spawnBubblesForRimeRound(target, unitLetters).toImmutableList()
        } else {
            spawnBubblesForRound(target, unitLetters).toImmutableList()
        }
        bubblesCache[round] = fresh
        return fresh
    }

    private fun starsFor(score: Int): Int = when {
        score >= 10 -> 5
        score >= 8 -> 4
        score >= 5 -> 3
        score >= 2 -> 2
        else -> 1
    }

    private companion object {
        const val TAG = "BubblePopViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val ROUND_DURATION_MS = 30_000L
        const val TICK_MS = 100L
        const val TARGET_POOL = 10
        const val ROUND_START_DELAY_MS = 500L
        const val GUIDE_AUDIO_MAX_MS = 4_000L

        /** Nghỉ giữa lúc câu hỏi dứt và lúc thẻ vần hiện ra. */
        const val REVEAL_DELAY_MS = 1_000L

        /**
         * Thẻ vần nằm trên màn bao lâu — **núm chỉnh nhịp của bước này**.
         *
         * Dài hơn thì bé dễ nhớ mặt vần nhưng chờ lâu trước mỗi vòng; ngắn hơn thì ngược
         * lại. 2s là mốc user chốt 2026-08-31, đo bằng mắt trên máy ảo.
         */
        const val REVEAL_DURATION_MS = 2_000L
        val PRAISE_POOL = listOf("praise_nice", "praise_great_job", "praise_well_done")
        val FINAL_PRAISE_POOL = listOf("praise_great_job", "praise_well_done", "praise_you_got_it")
    }
}

/**
 * Level 1 dạy CHỮ CÁI, Level 2+ dạy VẦN — trò chơi phải đổi cả nhãn lẫn audio theo đó.
 *
 * Nhận biết qua số cấp độ trong lesson id ("L2U1_am"), không qua hình dạng của `letter`:
 * mã vần hiện là "SHORT-A-AM" nhưng đó là quy ước dữ liệu có thể đổi, còn số cấp độ thì không.
 */
private fun PhonicsLesson.isRimeLesson(): Boolean =
    (LESSON_LEVEL_REGEX.find(id)?.groupValues?.get(1)?.toIntOrNull() ?: 1) >= FIRST_RIME_LEVEL

/** Cấp đầu tiên có vần BAO cần loại khỏi vòng chơi — xem [dropUmbrellaPatterns]. */
private fun PhonicsLesson.hasUmbrellaPatterns(): Boolean =
    (LESSON_LEVEL_REGEX.find(id)?.groupValues?.get(1)?.toIntOrNull() ?: 1) >= FIRST_UMBRELLA_LEVEL

/**
 * Mục tiêu của các vòng chơi trong unit.
 *
 * Level 1: chữ cái viết hoa ("A", "B", "C") — đúng như cũ.
 * Level 2+: các VẦN lấy từ `displayLetter`, KHÔNG phải `letter`. `letter` là mã dữ liệu
 * ("SHORT-A-AM") và chính nó từng lọt vào bong bóng thành chuỗi "SHORT-A" (user báo
 * 2026-08-12). Một lesson có thể dạy hai vần ("ad ag") nên phải tách theo dấu cách —
 * vì thế unit 2 có 4 vòng dù chỉ có 3 lesson.
 * Level 3+: lọc thêm một bước, xem [dropUmbrellaPatterns].
 */
private fun List<PhonicsLesson>.gameTargets(): List<String> =
    if (firstOrNull()?.isRimeLesson() == true) {
        val patterns = flatMap { lesson -> lesson.displayLetter.trim().lowercase().split(' ') }
            .filter { it.isNotEmpty() }
            .distinct()
        if (firstOrNull()?.hasUmbrellaPatterns() == true) dropUmbrellaPatterns(patterns) else patterns
    } else {
        map { it.letter.uppercase() }
    }

/**
 * Bỏ những vần ÔM TRỌN unit khỏi danh sách vòng chơi.
 *
 * `a_e` của unit 1 và `i_e` của unit 2 là vần BAO — mọi từ trong unit đều khớp (`tape`
 * `game` `cake` … đều tách ra nguyên âm `a_e`). Vòng đó không có "từ của riêng nó",
 * trái với luật mỗi vòng ứng với một vần, nên user chốt bỏ (2026-08-31).
 *
 * Nhận diện bằng dữ liệu chứ không liệt kê `a_e`/`i_e` bằng tay: cấp 4-5 còn vần bao
 * khác, và mỗi lần liệt kê tay là một lần quên. Unit 3 giữ đủ hai vòng vì `o_e` chỉ
 * khớp 4/12 từ và `u_e` khớp 8/12 — không vần nào ôm trọn.
 */
private fun List<PhonicsLesson>.dropUmbrellaPatterns(patterns: List<String>): List<String> {
    val allWords = flatMap { it.words }.map { it.word }
    val kept = patterns.filterNot { p -> allWords.all { wordHasPattern(it, p) } }
    // Không giữ được vần nào (dữ liệu lạ) thì thà chơi bằng bộ cũ còn hơn màn trống.
    return kept.ifEmpty { patterns }
}

private val LESSON_LEVEL_REGEX = Regex("""^L(\d+)U""")
private const val FIRST_RIME_LEVEL = 2
private const val FIRST_UMBRELLA_LEVEL = 3

@Immutable
internal data class BubblePopUiState(
    val unitLetters: ImmutableList<String>,
    val roundIndex: Int,
    val totalRounds: Int,
    val targetLetter: String,
    /** Tên file audio của mục tiêu — khác [targetLetter] khi nhãn đụng nhau, xem [rimeAudioKeys]. */
    val targetAudioKey: String = "",
    val bubbles: ImmutableList<BubbleSpec>,
    val popCount: Int,
    val targetPool: Int,
    val timeRemainingMs: Long,
    val roundDurationMs: Long,
    val isRoundComplete: Boolean,
    val isGameComplete: Boolean,
    val roundStars: Int,
    val isGuidePlaying: Boolean = false,
    /** True cho Level 2+: mục tiêu là VẦN, nhãn + audio đi theo bộ vần. */
    val isRimeMode: Boolean = false,
    /** Vần hiện giữa màn trước khi bong bóng nổi lên; null = đang không hiện. */
    val revealedTarget: String? = null,
) {
    companion object {
        val Empty = BubblePopUiState(
            unitLetters = persistentListOf(),
            roundIndex = 0,
            totalRounds = 1,
            targetLetter = "",
            bubbles = persistentListOf(),
            popCount = 0,
            targetPool = 10,
            timeRemainingMs = 30_000L,
            roundDurationMs = 30_000L,
            isRoundComplete = false,
            isGameComplete = false,
            roundStars = 0,
        )
    }
}
