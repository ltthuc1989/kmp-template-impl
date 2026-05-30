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
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.step.common.soundIntroRef

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

    private val lessonsFlow = MutableStateFlow<List<PhonicsLesson>>(emptyList())
    private val roundIndex = MutableStateFlow(0)
    private val popCount = MutableStateFlow(0)
    private val timeRemainingMs = MutableStateFlow(ROUND_DURATION_MS)
    private val roundComplete = MutableStateFlow(false)
    private val gameComplete = MutableStateFlow(false)
    private val roundStars = MutableStateFlow(0)

    private val bubblesCache = mutableMapOf<Int, ImmutableList<BubbleSpec>>()

    private var timerJob: Job? = null

    val screenState: StateFlow<ScreenState<BubblePopUiState>> =
        combine(
            unitRepository.observeLessons(unitId).onEach { lessonsFlow.value = it },
            combine(roundIndex, popCount, timeRemainingMs) { r, p, t -> Triple(r, p, t) },
            combine(roundComplete, gameComplete, roundStars) { rc, gc, rs -> Triple(rc, gc, rs) },
        ) { lessons, roundData, statusData ->
            val (round, popped, time) = roundData
            val (isRoundDone, isGameDone, stars) = statusData
            if (lessons.isEmpty()) {
                ScreenState.Error(message = Res.string.error_no_data)
            } else {
                val unitLetters = lessons.map { it.letter.uppercase() }.toImmutableList()
                val totalRounds = unitLetters.size.coerceAtLeast(1)
                val clampedRound = round.coerceIn(0, totalRounds - 1)
                val target = unitLetters[clampedRound]
                val bubbles = bubblesForRound(target, unitLetters, clampedRound)
                ScreenState.Idle(
                    BubblePopUiState(
                        unitLetters = unitLetters,
                        roundIndex = clampedRound,
                        totalRounds = totalRounds,
                        targetLetter = target,
                        bubbles = bubbles,
                        popCount = popped,
                        targetPool = TARGET_POOL,
                        timeRemainingMs = time,
                        roundDurationMs = ROUND_DURATION_MS,
                        isRoundComplete = isRoundDone,
                        isGameComplete = isGameDone,
                        roundStars = stars,
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
            // Each time a new active round starts, ensure timer is running.
            // (Audio guide prompt deferred — currently no asset; per-tap letter sound plays
            // instead via onBubbleTapped.)
            screenState.collect { state ->
                if (state is ScreenState.Idle) {
                    val ui = state.data
                    if (!ui.isRoundComplete && !ui.isGameComplete) {
                        ensureTimerRunning()
                    }
                }
            }
        }
    }

    /**
     * Plays the phoneme audio for any letter present in the unit's lessons (used by both
     * target and distractor taps). If the letter isn't in this unit (rare — distractor
     * sourced from random alphabet), no audio plays.
     */
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
        // Always play the letter's phoneme on any tap — kid learns the sound by exploring.
        playLetterSound(spec.letter)
        if (isCorrect) {
            sfxController.playSfx("correct")
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
        audioRepository.stop()
    }

    private fun totalRoundsSnapshot(): Int =
        (screenState.value as? ScreenState.Idle)?.data?.totalRounds ?: 1

    private fun bubblesForRound(
        target: String,
        unitLetters: List<String>,
        round: Int,
    ): ImmutableList<BubbleSpec> {
        bubblesCache[round]?.let { return it }
        val fresh = spawnBubblesForRound(target, unitLetters).toImmutableList()
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
        val PRAISE_POOL = listOf("praise_nice", "praise_great_job", "praise_well_done")
        val FINAL_PRAISE_POOL = listOf("praise_great_job", "praise_well_done", "praise_you_got_it")
    }
}

@Immutable
internal data class BubblePopUiState(
    val unitLetters: ImmutableList<String>,
    val roundIndex: Int,
    val totalRounds: Int,
    val targetLetter: String,
    val bubbles: ImmutableList<BubbleSpec>,
    val popCount: Int,
    val targetPool: Int,
    val timeRemainingMs: Long,
    val roundDurationMs: Long,
    val isRoundComplete: Boolean,
    val isGameComplete: Boolean,
    val roundStars: Int,
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
