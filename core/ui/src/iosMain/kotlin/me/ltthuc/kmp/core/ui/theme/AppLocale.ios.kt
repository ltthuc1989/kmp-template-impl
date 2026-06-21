package me.ltthuc.kmp.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * iOS is best-effort: Compose Resources on iOS reads the preferred language, which we steer via the
 * `AppleLanguages` user default. A full, reliable switch can require an app relaunch — Android is the
 * Mốc 1 verification target. The [staticCompositionLocalOf] mirror forces recomposition on change.
 */
actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val default: String = NSLocale.currentLocale.languageCode ?: "en"
    private val mirror = staticCompositionLocalOf { default }

    actual val deviceTag: String = default

    actual val current: String
        @Composable get() = mirror.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(LANG_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(listOf(value), LANG_KEY)
        }
        return mirror provides (value ?: default)
    }
}
