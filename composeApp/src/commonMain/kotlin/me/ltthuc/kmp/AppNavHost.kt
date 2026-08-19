package me.ltthuc.kmp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.ui.animation.NavigationTransitions
import me.ltthuc.kmp.core.ui.audio.rememberSilencingNavBackStack
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.view.AppBottomNavBar
import me.ltthuc.kmp.core.ui.screen.view.AppBottomNavTab
import me.ltthuc.kmp.core.ui.screen.view.FloatingNavVisibility
import me.ltthuc.kmp.core.ui.screen.view.LocalFloatingNavHeight
import me.ltthuc.kmp.core.ui.screen.view.LocalFloatingNavVisibility
import me.ltthuc.kmp.core.ui.theme.AppDimensions
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.billing.paywallEntry
import me.ltthuc.kmp.feature.home.homeEntry
import me.ltthuc.kmp.feature.learningpath.learningEntry
import me.ltthuc.kmp.feature.onboarding.onboardingEntry
import me.ltthuc.kmp.feature.review.reviewEntry
import me.ltthuc.kmp.feature.setting.oss.settingLicenseEntry
import me.ltthuc.kmp.feature.setting.settingEntry
import org.koin.compose.koinInject

@Composable
internal fun AppNavHost(
    startDestinations: List<Destination>,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Destination.config, *startDestinations.toTypedArray())
    // What every screen pushes and pops through: it stops the leaving screen's audio as the tap
    // happens, instead of when navigation finally disposes that screen — several hundred ms later,
    // by which time the child is already looking at (and hearing it over) the next screen.
    val navBackStack = rememberSilencingNavBackStack(backStack)
    val floatingNavVisibility = remember { FloatingNavVisibility() }

    CompositionLocalProvider(
        LocalNavBackStack provides navBackStack,
        LocalFloatingNavVisibility provides floatingNavVisibility,
    ) {
        val currentTab = navBackStack.lastOrNull().toBottomNavTabOrNull()

        // Persist the last screen so a relaunch restores the right place (Lesson Map / Unit list /
        // Level list) instead of always restarting. Steps/Story/Games resolve to their Lesson Map.
        val appSettingRepository = koinInject<AppSettingRepository>()
        val topDestination = navBackStack.lastOrNull()
        LaunchedEffect(topDestination) {
            when (val top = topDestination) {
                Destination.Home ->
                    appSettingRepository.setLastScreen(AppSetting.LastScreen.LEVEL_LIST, "", "")
                is Destination.Learning.UnitSelection ->
                    appSettingRepository.setLastScreen(AppSetting.LastScreen.UNIT_LIST, top.levelId, "")
                is Destination.Learning.LessonMap ->
                    appSettingRepository.setLastScreen(AppSetting.LastScreen.LESSON_MAP, top.levelId, top.unitId)
                is Destination.Learning.Step ->
                    appSettingRepository.setLastScreen(AppSetting.LastScreen.LESSON_MAP, top.levelId, top.unitId)
                is Destination.Learning.UnitStory ->
                    appSettingRepository.setLastScreen(AppSetting.LastScreen.LESSON_MAP, top.levelId, top.unitId)
                is Destination.Learning.UnitGame ->
                    appSettingRepository.setLastScreen(AppSetting.LastScreen.LESSON_MAP, top.levelId, top.unitId)
                else -> Unit
            }
        }

        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                // A screen that covers itself with a full-screen overlay (the parental gate) also
                // hides the pill: it is hosted here, above the entries, so it would float on top of
                // that overlay instead of being covered by it.
                if (currentTab != null && floatingNavVisibility.isVisible) {
                    AppBottomNavBar(currentTab = currentTab)
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    // Only the top padding is applied: the pill nav floats, so screens run the full
                    // height and their content passes UNDER it. Its height is handed down instead,
                    // for screens to pad their scrollable content and fade it out across.
                    .padding(top = innerPadding.calculateTopPadding()),
                contentAlignment = Alignment.TopCenter,
            ) {
                // Kid screens are always English. Re-providing "en" here re-runs the locale
                // side-effect on every navigation, so returning from Settings (which overrides the
                // locale to the chosen language) resets kid screens back to English. Parent entries
                // (Settings/Paywall/Onboarding) override back to LocalAppLanguage inside their entries.
                CompositionLocalProvider(
                    LocalAppLocale provides "en",
                    LocalFloatingNavHeight provides innerPadding.calculateBottomPadding(),
                ) {
                    NavDisplay(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = AppDimensions.ContentMaxWidth),
                        backStack = backStack,
                        // Same pop as the default, routed through the silencing wrapper so a system
                        // back / predictive-back gesture cuts the audio too.
                        onBack = {
                            if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                        },
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
}

private fun NavKey?.toBottomNavTabOrNull(): AppBottomNavTab? = when (this) {
    Destination.Home -> AppBottomNavTab.Home
    // Settings is reached from the top-bar gear, not a bottom tab — no bottom nav on that screen.
    else -> null
}
