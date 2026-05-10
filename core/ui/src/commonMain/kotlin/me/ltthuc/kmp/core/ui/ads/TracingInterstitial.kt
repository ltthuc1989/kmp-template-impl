package me.ltthuc.kmp.core.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexilabs.basic.ads.AdState
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.rememberInterstitialAd
import io.github.aakira.napier.Napier
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource
import me.ltthuc.kmp.core.model.AppConfig
import org.koin.compose.koinInject

/**
 * Shows fullscreen interstitial when user enters Tracing screen, gated by per-process
 * frequency cap (3 minutes). Balance between revenue and kid-friendly pacing —
 * gives ~3-4 ads per typical 10-15 min phonics session.
 *
 * Uses `rememberInterstitialAd` (auto-loads in background) + `LaunchedEffect` watching state,
 * showing the ad once it reaches [AdState.READY]. Direct `InterstitialAd()` composable
 * crashes if state isn't READY when composition runs — must be guarded with state observation.
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun TracingInterstitial() {
    val appConfig: AppConfig = koinInject()
    val shouldAttempt = remember {
        val last = TracingAdGate.lastShownMark
        val canShow = last == null || last.elapsedNow() >= INTERSTITIAL_COOLDOWN
        if (canShow) TracingAdGate.lastShownMark = TimeSource.Monotonic.markNow()
        canShow
    }
    if (!shouldAttempt) return

    val ad by rememberInterstitialAd(
        adUnitId = appConfig.adMobInterstitialAdUnitId,
        onFailure = { e ->
            Napier.w(tag = "TracingInterstitial") { "Load fail: ${e.message}" }
        },
    )
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(ad.state) {
        if (!shown && ad.state == AdState.READY) {
            shown = true
            ad.setListeners(
                onFailure = { e ->
                    Napier.w(tag = "TracingInterstitial") { "Show fail: ${e.message}" }
                },
                onDismissed = {},
            )
            ad.show()
        }
    }
}

private val INTERSTITIAL_COOLDOWN = 3.minutes

private object TracingAdGate {
    var lastShownMark: TimeSource.Monotonic.ValueTimeMark? = null
}
