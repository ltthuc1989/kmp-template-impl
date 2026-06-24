package me.ltthuc.kmp.core.ui

/**
 * True for DEBUG builds, false for RELEASE. Platform-specific (`BuildConfig.DEBUG` on Android).
 *
 * Use to gate developer-only UI (e.g. the Settings "Other" / dev-tools section) so it never ships
 * to end users in a release build.
 */
expect val isDebugBuild: Boolean
