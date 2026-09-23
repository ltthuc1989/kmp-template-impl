package me.ltthuc.kmp.feature.setting

/**
 * Hidden "open the paid content for review" affordance.
 *
 * Google Play's App access declaration requires a way for a reviewer to reach anything sitting
 * behind a purchase. This app has no accounts, so there is no test login to hand over: instead
 * the version line in the Settings footer opens the PIN prompt after [REVIEWER_UNLOCK_TAPS]
 * quick taps, and the PIN goes into the App access instructions.
 *
 * Deliberately NOT developer mode. That flag also reports every unit as Completed and reveals
 * levels whose content has not shipped (`LevelRepository.LAUNCHED_PREMIUM_LEVELS`) — a reviewer
 * would land in empty screens. Writing the owned set instead shows the app exactly as a paying
 * parent sees it.
 *
 * The rules live here as pure functions so they are testable without Compose or a DataStore
 * (playbook R7).
 */

/** Taps on the version line that open the PIN prompt. */
internal const val REVIEWER_UNLOCK_TAPS = 7

/** A tap later than this after the previous one starts the count over. */
internal const val REVIEWER_TAP_WINDOW_MS = 3_000L

/**
 * Tap counter with a decay window, so taps spread across a long visit to Settings never add up
 * into an unlock prompt nobody asked for.
 *
 * @param sinceLastTapMillis time since the previous tap; pass [Long.MAX_VALUE] for the first one.
 */
internal fun advanceTapCount(
    previous: Int,
    sinceLastTapMillis: Long,
    windowMillis: Long = REVIEWER_TAP_WINDOW_MS,
): Int = if (sinceLastTapMillis > windowMillis) 1 else previous + 1

/** What a correct PIN should do, decided from the level list the screen holds right now. */
internal sealed interface ReviewerUnlockOutcome {

    /** [levelIds] is never empty — that case is [NoLevelsLoaded]. */
    data class Unlocked(val levelIds: Set<String>) : ReviewerUnlockOutcome

    /**
     * The level list had not arrived yet (`levels` starts at `emptyList()` and fills in from
     * Room). Writing the empty set here would report success and unlock nothing — the silent
     * failure playbook rule R2 exists for.
     */
    data object NoLevelsLoaded : ReviewerUnlockOutcome
}

internal fun reviewerUnlockOutcome(levelIds: Set<String>): ReviewerUnlockOutcome =
    if (levelIds.isEmpty()) {
        ReviewerUnlockOutcome.NoLevelsLoaded
    } else {
        ReviewerUnlockOutcome.Unlocked(levelIds)
    }
