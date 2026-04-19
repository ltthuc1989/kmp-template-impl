package me.matsumo.grabee.core.ui.screen.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.persistentListOf
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.nav_home
import me.matsumo.grabee.core.resource.nav_review
import me.matsumo.grabee.core.resource.nav_settings
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.StringResource

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
    Review(
        key = "review",
        destination = Destination.Review,
        labelRes = Res.string.nav_review,
        iconInactive = Icons.AutoMirrored.Outlined.MenuBook,
        iconActive = Icons.AutoMirrored.Filled.MenuBook,
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
                key = AppBottomNavTab.Review.key,
                label = AppBottomNavTab.Review.labelRes,
                iconInactive = AppBottomNavTab.Review.iconInactive,
                iconActive = AppBottomNavTab.Review.iconActive,
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
            navBackStack.clear()
            navBackStack.add(target.destination)
        },
    )
}
