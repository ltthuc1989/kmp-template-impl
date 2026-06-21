package me.ltthuc.kmp.core.model

/**
 * Mốc 1 monetization gate. Per launch plan Part F.6b:
 * - Mốc 1 ships Level 1 forever-free, no ads, no IAP, no paywall UI.
 * - Mốc 2 adds Level 2 + paywall — flip flag to `true` to restore ads + paywall.
 *
 * Flag is read by BottomBannerAd, LearningInterstitial, RewardedSkipLauncher,
 * SettingPaywallSection, and Home level-lock flow.
 */
const val MONETIZATION_ENABLED = true

/**
 * Number of free sample units at the start of every level (unit orderIndex < this value).
 * Units from this index onward require owning the level (per-level $5 or full bundle $20),
 * unless [MONETIZATION_ENABLED] is false (then everything stays open as in Mốc 1).
 */
const val FREE_UNITS_PER_LEVEL = 2
