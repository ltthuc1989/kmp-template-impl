package me.ltthuc.kmp.core.common.share

/**
 * Triggers the platform in-app review prompt (Play In-App Review / iOS SKStoreReviewController).
 * Falls back to opening the store listing when in-app review is unavailable.
 *
 * Note: Play in-app review only renders for builds installed from the Play Store; in debug it
 * no-ops or falls back to the store listing.
 */
expect class Reviewer {
    fun requestReview()
}
