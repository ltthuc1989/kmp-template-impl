package me.ltthuc.kmp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.DependsOnGoogleUserMessagingPlatform
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.GrabeeTheme

@OptIn(DependsOnGoogleMobileAds::class, DependsOnGoogleUserMessagingPlatform::class)
@Composable
internal fun GrabeeApp(
    setting: AppSetting,
    modifier: Modifier = Modifier,
) {
    SetupCoil()
    BasicAds.Initialize()

    // V1 ships Level 1 only — skip the level-selection Home screen and drop the
    // user directly into the alphabet unit list. When L2-L5 ship, restore Home.
    val startDestination: Destination =
        if (setting.hasSeenOnboarding) {
            Destination.Learning.UnitSelection(levelId = LEVEL_1_ID)
        } else {
            Destination.Onboarding
        }

    GrabeeTheme(setting) {
        AppNavHost(
            startDestination = startDestination,
            modifier = modifier,
        )
    }
}

private const val LEVEL_1_ID = "L1"

@Composable
private fun SetupCoil() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                addPlatformFileSupport()
            }
            .crossfade(true)
            .build()
    }
}
