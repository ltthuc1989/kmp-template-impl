package me.ltthuc.kmp.feature.setting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.dialog.ParentalGateScreen
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack

fun EntryProviderScope<NavKey>.settingEntry() {
    entry<Destination.Setting.Gate> {
        val navBackStack = LocalNavBackStack.current
        CompositionLocalProvider(LocalAppLocale provides LocalAppLanguage.current) {
            ParentalGateScreen(
                modifier = Modifier.fillMaxSize(),
                onPass = {
                    navBackStack.clear()
                    navBackStack.add(Destination.Setting.Root)
                },
                onDismiss = {
                    navBackStack.clear()
                    navBackStack.add(Destination.Home)
                },
            )
        }
    }
    entry<Destination.Setting.Root> {
        CompositionLocalProvider(LocalAppLocale provides LocalAppLanguage.current) {
            SettingScreen(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
