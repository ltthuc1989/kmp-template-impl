package me.matsumo.grabee.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import me.matsumo.grabee.core.model.AppConfig
import me.matsumo.grabee.core.model.AppSetting

val LocalAppSetting = staticCompositionLocalOf {
    AppSetting.DEFAULT
}

val LocalAppConfig = staticCompositionLocalOf<AppConfig> {
    error("No AppConfig provided")
}
