package me.ltthuc.kmp.core.billing

// iOS is not under active local testing yet; default to real RevenueCat billing.
// Flip to a debug check (e.g. Platform.isDebugBinary) when iOS fake-billing testing is needed.
internal actual val isDebugBuild: Boolean = false
