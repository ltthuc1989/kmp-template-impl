package me.ltthuc.kmp.core.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    private var default: Locale? = null

    // Device locale captured at class init — before any override mutates Locale.getDefault().
    private val device: Locale = Locale.getDefault()

    actual val deviceTag: String = device.toLanguageTag()

    actual val current: String
        @Composable get() = Locale.getDefault().toLanguageTag()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current

        // Remember the original device locale so `null` (System) can restore it.
        if (default == null) {
            default = Locale.getDefault()
        }

        val new = if (value == null) default!! else Locale.forLanguageTag(value)
        Locale.setDefault(new)

        val newConfig = Configuration(configuration).apply { setLocale(new) }

        // Keep the platform resources in sync — some Android APIs read locale from here, not Compose.
        val resources = LocalContext.current.resources
        @Suppress("DEPRECATION")
        resources.updateConfiguration(newConfig, resources.displayMetrics)

        // Compose Resources reads Locale.current, which on Android derives from LocalConfiguration.
        return LocalConfiguration provides newConfig
    }
}
