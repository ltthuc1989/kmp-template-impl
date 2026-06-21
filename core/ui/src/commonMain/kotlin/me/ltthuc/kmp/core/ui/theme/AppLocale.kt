package me.ltthuc.kmp.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Overrides the locale used by Compose Resources at runtime so the app can switch its UI language
 * independently of the device setting.
 *
 * Compose Resources selects strings from [androidx.compose.ui.text.intl.Locale]`.current`
 * (see the library's `DefaultComposeEnvironment`). Since that environment is `internal`, the only
 * supported way to switch language at runtime is to override `Locale.current` from a platform
 * [androidx.compose.runtime.CompositionLocalProvider]. Pass a BCP-47 tag (`"en"`, `"vi"`) to force a
 * language, or `null` to follow the device locale.
 *
 * Usage: `CompositionLocalProvider(LocalAppLocale provides tag) { ... }`.
 */
expect object LocalAppLocale {
    val current: String
        @Composable get

    /** The original device locale tag (e.g. "vi-VN", "en-US"), captured once before any override. */
    val deviceTag: String

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
