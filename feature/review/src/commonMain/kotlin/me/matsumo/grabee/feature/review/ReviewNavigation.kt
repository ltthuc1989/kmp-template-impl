package me.matsumo.grabee.feature.review

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.matsumo.grabee.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.reviewEntry() {
    entry<Destination.Review> {
        ReviewScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
