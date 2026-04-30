package me.ltthuc.kmp.feature.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.screen.Destination

fun EntryProviderScope<NavKey>.onboardingEntry() {
    entry<Destination.Onboarding> {
        OnboardingScreen(
            modifier = Modifier.fillMaxSize(),
        )
    }
}
