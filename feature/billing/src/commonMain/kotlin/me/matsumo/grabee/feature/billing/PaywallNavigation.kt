package me.matsumo.grabee.feature.billing

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.grabee.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.paywallEntry() {
    entry<Destination.Paywall> { destination ->
        PaywallScreen(
            modifier = Modifier.fillMaxSize(),
            source = destination.source,
        )
    }
}
