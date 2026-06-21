package me.ltthuc.kmp.feature.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale

fun EntryProviderScope<NavKey>.onboardingEntry() {
    entry<Destination.Onboarding> {
        CompositionLocalProvider(LocalAppLocale provides LocalAppLanguage.current) {
            OnboardingScreen(
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
