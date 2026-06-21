package me.ltthuc.kmp.core.billing

/** True for DEBUG builds, false for RELEASE. Platform-specific (`BuildConfig.DEBUG` on Android). */
internal expect val isDebugBuild: Boolean

/**
 * Whether to serve billing from [FakeBillingDataSource] (in-memory) instead of real RevenueCat.
 *
 * Gated to **DEBUG builds**: the paywall + unlock flow works on any emulator with no store /
 * RevenueCat config, while RELEASE builds always use real RevenueCat billing. (Real Google Play
 * IAP doesn't work on sideloaded debug builds anyway — it needs an app installed via Play.)
 */
val USE_FAKE_BILLING: Boolean get() = isDebugBuild
