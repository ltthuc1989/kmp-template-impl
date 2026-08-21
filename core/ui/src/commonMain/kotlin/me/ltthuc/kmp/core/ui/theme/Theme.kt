package me.ltthuc.kmp.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.rememberDynamicColorScheme
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Theme
import org.koin.compose.koinInject

@Suppress("ModifierMissing")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GrabeeTheme(
    appSetting: AppSetting = AppSetting.DEFAULT,
    appConfig: AppConfig = koinInject(),
    content: @Composable () -> Unit,
) {
    val isDark = shouldUseDarkTheme(appSetting.theme)
    val colorScheme = rememberPaletteColorScheme(
        palette = appSetting.appThemePalette,
        isDark = isDark,
    )

    // Kid-facing screens render in English (immersion); only the parent screens override their text
    // locale back to [appLang]. Audio/voice prompts follow [appLang] everywhere via LocalAppLanguage.
    val appLang = resolveAppLanguage(appSetting.language, LocalAppLocale.deviceTag)
    CompositionLocalProvider(
        LocalAppLocale provides "en",
        LocalAppLanguage provides appLang,
        LocalAppSetting provides appSetting,
        LocalAppConfig provides appConfig,
        LocalPhonicsFontFamily provides rememberPhonicsFontFamily(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                content = content,
            )
        }
    }
}

@Composable
fun shouldUseDarkTheme(theme: Theme): Boolean {
    return when (theme) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }
}

@Composable
private fun rememberPaletteColorScheme(
    palette: AppThemePalette,
    isDark: Boolean,
): ColorScheme {
    if (!isDark) {
        return remember(palette) {
            when (palette) {
                AppThemePalette.PlayfulMentor -> PlayfulMentorLight
                AppThemePalette.FluidArchitect -> FluidArchitectLight
            }
        }
    }

    val seed = when (palette) {
        AppThemePalette.PlayfulMentor -> Color(0xFFAB2C5D)
        AppThemePalette.FluidArchitect -> Color(0xFF0058BC)
    }
    return rememberDynamicColorScheme(seedColor = seed, isDark = true)
}
