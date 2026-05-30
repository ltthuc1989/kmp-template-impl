package me.ltthuc.kmp.core.model

/**
 * App build-time configuration. Holds version info, developer PIN, and
 * platform-specific subscription API keys.
 *
 * Mốc 1: ad-related fields removed (no ad SDKs integrated). Archived in
 * marketing/mocs/moc1-ads-archive.md for Mốc 2 restoration.
 */
data class AppConfig(
    val versionName: String,
    val versionCode: Int,
    val developerPin: String,
    val purchaseAndroidApiKey: String?,
    val purchaseIosApiKey: String?,
)
