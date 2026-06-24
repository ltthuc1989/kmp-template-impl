package me.ltthuc.kmp.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.ForceEnglishLocale

fun EntryProviderScope<NavKey>.homeEntry() {
    entry<Destination.Home> {
        ForceEnglishLocale {
            HomeScreen(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
