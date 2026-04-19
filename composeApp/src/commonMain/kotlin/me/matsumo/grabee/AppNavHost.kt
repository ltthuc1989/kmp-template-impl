package me.matsumo.grabee

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import me.matsumo.grabee.core.ui.animation.NavigationTransitions
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.screen.view.AppBottomNavBar
import me.matsumo.grabee.core.ui.screen.view.AppBottomNavTab
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack
import me.matsumo.grabee.feature.billing.paywallEntry
import me.matsumo.grabee.feature.home.homeEntry
import me.matsumo.grabee.feature.learningpath.learningEntry
import me.matsumo.grabee.feature.onboarding.onboardingEntry
import me.matsumo.grabee.feature.review.reviewEntry
import me.matsumo.grabee.feature.setting.oss.settingLicenseEntry
import me.matsumo.grabee.feature.setting.settingEntry

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
            NavDisplay(
                modifier = Modifier.padding(innerPadding),
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

private fun NavKey?.toBottomNavTabOrNull(): AppBottomNavTab? = when (this) {
    Destination.Home -> AppBottomNavTab.Home
    Destination.Review -> AppBottomNavTab.Review
    Destination.Setting.Root -> AppBottomNavTab.Settings
    else -> null
}
