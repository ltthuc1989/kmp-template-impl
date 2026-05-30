package me.ltthuc.kmp.core.model

/**
 * Mốc 1 monetization gate. Per launch plan Part F.6b:
 * - Mốc 1 ships Level 1 forever-free, no ads, no IAP, no paywall UI.
 * - Mốc 2 adds Level 2 + paywall — flip flag to `true` to restore ads + paywall.
 *
 * Flag is read by BottomBannerAd, LearningInterstitial, RewardedSkipLauncher,
 * SettingPaywallSection, and Home level-lock flow.
 */
const val MONETIZATION_ENABLED = false
