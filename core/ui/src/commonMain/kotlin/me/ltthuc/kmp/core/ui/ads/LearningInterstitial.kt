package me.ltthuc.kmp.core.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lexilabs.basic.ads.AdState
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.rememberInterstitialAd
import io.github.aakira.napier.Napier
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.repository.AppSettingRepository
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

/**
 * Shows fullscreen interstitial between learning sessions (e.g. on UnitCompleteScreen),
 * gated by per-process frequency cap (3 minutes). Balance between revenue and kid-friendly
 * pacing — gives ~3-4 ads per typical 10-15 min phonics session.
 *
 * Premium users (`hasPrivilege`) see no ads.
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun LearningInterstitial() {
    val appSettingRepository: AppSettingRepository = koinInject()
    val setting by appSettingRepository.setting.collectAsStateWithLifecycle()
    if (setting.hasPrivilege) return

    val appConfig: AppConfig = koinInject()
    val shouldAttempt = remember {
        val last = LearningAdGate.lastShownMark
        val canShow = last == null || last.elapsedNow() >= INTERSTITIAL_COOLDOWN
        if (canShow) LearningAdGate.lastShownMark = TimeSource.Monotonic.markNow()
        canShow
    }
    if (!shouldAttempt) return

    val ad by rememberInterstitialAd(
        adUnitId = appConfig.adMobInterstitialAdUnitId,
        onFailure = { e ->
            Napier.w(tag = "LearningInterstitial") { "Load fail: ${e.message}" }
        },
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(ad.state) {
        if (!shown && ad.state == AdState.READY) {
            shown = true
            ad.setListeners(
                onFailure = { e ->
                    Napier.w(tag = "LearningInterstitial") { "Show fail: ${e.message}" }
                },
                onDismissed = {},
            )
            ad.show()
        }
    }
}

private val INTERSTITIAL_COOLDOWN = 3.minutes

private object LearningAdGate {
    var lastShownMark: TimeSource.Monotonic.ValueTimeMark? = null
}
