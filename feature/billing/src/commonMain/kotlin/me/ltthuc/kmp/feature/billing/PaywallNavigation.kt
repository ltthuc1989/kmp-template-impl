package me.ltthuc.kmp.feature.billing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale

fun EntryProviderScope<NavKey>.paywallEntry() {
    entry<Destination.Paywall> { destination ->
        CompositionLocalProvider(LocalAppLocale provides LocalAppLanguage.current) {
            PaywallScreen(
                modifier = Modifier.fillMaxSize(),
                source = destination.source,
                levelId = destination.levelId,
                gatedAlready = destination.gatedAlready,
            )
        }
    }
}
