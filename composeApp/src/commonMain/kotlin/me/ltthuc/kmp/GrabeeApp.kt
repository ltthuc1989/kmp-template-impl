package me.ltthuc.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import app.lexilabs.basic.ads.BasicAds
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.DependsOnGoogleUserMessagingPlatform
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.GrabeeTheme
import org.koin.compose.koinInject

@OptIn(DependsOnGoogleMobileAds::class, DependsOnGoogleUserMessagingPlatform::class)
@Composable
internal fun GrabeeApp(
    setting: AppSetting,
    modifier: Modifier = Modifier,
) {
    SetupCoil()
    BasicAds.Initialize()

    // Mirror user audio settings into the SFX layer. Cheap (writes to MutableStateFlow)
    // so we run it on every recomposition keyed by the relevant fields.
    val sfx = koinInject<SfxController>()
    LaunchedEffect(setting.sfxEnabled, setting.voiceEnabled, setting.musicEnabled, setting.globalMuted) {
        sfx.configure(
            sfxEnabled = setting.sfxEnabled,
            voiceEnabled = setting.voiceEnabled,
            musicEnabled = setting.musicEnabled,
            globalMuted = setting.globalMuted,
        )
    }

    val startDestination: Destination =
        if (setting.hasSeenOnboarding) {
            Destination.Home
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
