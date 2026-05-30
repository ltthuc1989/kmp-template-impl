package me.ltthuc.kmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import me.ltthuc.kmp.core.ui.animation.NavigationTransitions
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.view.AppBottomNavBar
import me.ltthuc.kmp.core.ui.screen.view.AppBottomNavTab
import me.ltthuc.kmp.core.ui.theme.AppDimensions
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.billing.paywallEntry
import me.ltthuc.kmp.feature.home.homeEntry
import me.ltthuc.kmp.feature.learningpath.learningEntry
import me.ltthuc.kmp.feature.onboarding.onboardingEntry
import me.ltthuc.kmp.feature.review.reviewEntry
import me.ltthuc.kmp.feature.setting.oss.settingLicenseEntry
import me.ltthuc.kmp.feature.setting.settingEntry

@Composable
internal fun AppNavHost(
    startDestination: Destination,
    modifier: Modifier = Modifier,
) {
    val navBackStack = rememberNavBackStack(Destination.config, startDestination)

    CompositionLocalProvider(
        LocalNavBackStack provides navBackStack,
    ) {
        val currentTab = navBackStack.lastOrNull().toBottomNavTabOrNull()

        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentTab != null) {
                    AppBottomNavBar(currentTab = currentTab)
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = AppDimensions.ContentMaxWidth),
                    backStack = navBackStack,
                    entryProvider = entryProvider {
                        homeEntry()
                        paywallEntry()
                        settingEntry()
                        settingLicenseEntry()
                        learningEntry()
                        onboardingEntry()
                        reviewEntry()
                    },
                    transitionSpec = { NavigationTransitions.forwardTransition },
                    popTransitionSpec = { NavigationTransitions.backwardTransition },
                    predictivePopTransitionSpec = { NavigationTransitions.backwardTransition },
                )
            }
        }
    }
}

private fun NavKey?.toBottomNavTabOrNull(): AppBottomNavTab? = when (this) {
    Destination.Home -> AppBottomNavTab.Home
    Destination.Setting.Root -> AppBottomNavTab.Settings
    else -> null
}
