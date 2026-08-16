package me.ltthuc.kmp.core.ui.screen.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.nav_home
import me.ltthuc.kmp.core.resource.nav_settings
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.StringResource

/**
 * How much of the screen bottom the floating pill nav covers — 0 on screens that don't show it.
 *
 * The pill floats OVER the content instead of reserving a strip of layout, so a screen that scrolls
 * adds this to its bottom content padding (so the last item can still be scrolled clear of the pill)
 * and fades content out across it with [me.ltthuc.kmp.core.ui.utils.fadeOutBottom].
 */
val LocalFloatingNavHeight = compositionLocalOf { 0.dp }

enum class AppBottomNavTab(
    val key: String,
    val destination: Destination,
    val labelRes: StringResource,
    val iconInactive: ImageVector,
    val iconActive: ImageVector,
) {
    Home(
        key = "home",
        destination = Destination.Home,
        labelRes = Res.string.nav_home,
        iconInactive = Icons.Outlined.Home,
        iconActive = Icons.Filled.Home,
    ),
    Settings(
        key = "settings",
        destination = Destination.Setting.Root,
        labelRes = Res.string.nav_settings,
        iconInactive = Icons.Outlined.Settings,
        iconActive = Icons.Filled.Settings,
    ),
}

@Composable
fun AppBottomNavBar(
    currentTab: AppBottomNavTab,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val items = remember {
        persistentListOf(
            FloatingPillNavItem(
                key = AppBottomNavTab.Home.key,
                label = AppBottomNavTab.Home.labelRes,
                iconInactive = AppBottomNavTab.Home.iconInactive,
                iconActive = AppBottomNavTab.Home.iconActive,
            ),
            FloatingPillNavItem(
                key = AppBottomNavTab.Settings.key,
                label = AppBottomNavTab.Settings.labelRes,
                iconInactive = AppBottomNavTab.Settings.iconInactive,
                iconActive = AppBottomNavTab.Settings.iconActive,
            ),
        )
    }

    FloatingPillNavBar(
        modifier = modifier,
        items = items,
        selectedKey = currentTab.key,
        onItemSelected = { item ->
            val target = AppBottomNavTab.entries.firstOrNull { it.key == item.key } ?: return@FloatingPillNavBar
            if (target == currentTab) return@FloatingPillNavBar
            // Settings is gated behind a parental gate; push the gate instead of Settings itself.
            val destination = if (target == AppBottomNavTab.Settings) {
                Destination.Setting.Gate
            } else {
                target.destination
            }
            navBackStack.clear()
            navBackStack.add(destination)
        },
    )
}
