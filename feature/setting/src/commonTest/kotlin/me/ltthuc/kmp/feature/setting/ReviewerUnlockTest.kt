package me.ltthuc.kmp.feature.setting

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReviewerUnlockTest {

    @Test
    fun firstTapStartsTheCount() {
        assertEquals(1, advanceTapCount(previous = 0, sinceLastTapMillis = Long.MAX_VALUE))
    }

    @Test
    fun tapsInsideWindowAccumulate() {
        var count = 0
        repeat(REVIEWER_UNLOCK_TAPS) {
            count = advanceTapCount(count, sinceLastTapMillis = 200)
        }
        assertEquals(REVIEWER_UNLOCK_TAPS, count)
    }

    /** A parent poking the footer once a minute must never reach the prompt. */
    @Test
    fun tapsOutsideWindowRestart() {
        var count = 0
        repeat(REVIEWER_UNLOCK_TAPS * 2) {
            count = advanceTapCount(count, sinceLastTapMillis = REVIEWER_TAP_WINDOW_MS + 1)
        }
        assertEquals(1, count)
    }

    @Test
    fun oneSlowTapResetsAPartialRun() {
        var count = 0
        repeat(5) { count = advanceTapCount(count, sinceLastTapMillis = 100) }
        count = advanceTapCount(count, sinceLastTapMillis = REVIEWER_TAP_WINDOW_MS + 1)
        assertEquals(1, count)
    }

    @Test
    fun unlocksEveryLoadedLevel() {
        val outcome = reviewerUnlockOutcome(setOf("L1", "L2", "L3", "L4"))
        assertTrue(outcome is ReviewerUnlockOutcome.Unlocked)
        assertEquals(setOf("L1", "L2", "L3", "L4"), outcome.levelIds)
    }

    /**
     * The whole point of the type: an empty level list must not be written as "you now own
     * nothing" and reported as success.
     */
    @Test
    fun noLevelsLoadedIsAFailureNotAnEmptyUnlock() {
        assertEquals(ReviewerUnlockOutcome.NoLevelsLoaded, reviewerUnlockOutcome(emptySet()))
    }
}
