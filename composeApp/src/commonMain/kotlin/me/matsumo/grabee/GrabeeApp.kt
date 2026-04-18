package me.matsumo.grabee

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.DependsOnGoogleUserMessagingPlatform
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import me.matsumo.grabee.core.model.AppSetting
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.GrabeeTheme

@OptIn(DependsOnGoogleMobileAds::class, DependsOnGoogleUserMessagingPlatform::class)
@Composable
internal fun GrabeeApp(
    setting: AppSetting,
    modifier: Modifier = Modifier,
) {
    SetupCoil()
    BasicAds.Initialize()

    val startDestination: Destination =
        if (setting.hasSeenOnboarding) Destination.Home else Destination.Onboarding

    GrabeeTheme(setting) {
        AppNavHost(
            startDestination = startDestination,
            modifier = modifier,
        )
    }
}

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
