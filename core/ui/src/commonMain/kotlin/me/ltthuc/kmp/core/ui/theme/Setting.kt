package me.ltthuc.kmp.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.model.AppSetting

val LocalAppSetting = staticCompositionLocalOf {
    AppSetting.DEFAULT
}

val LocalAppConfig = staticCompositionLocalOf<AppConfig> {
    error("No AppConfig provided")
}
