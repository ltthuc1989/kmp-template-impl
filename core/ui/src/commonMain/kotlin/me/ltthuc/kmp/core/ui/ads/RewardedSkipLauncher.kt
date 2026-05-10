package me.ltthuc.kmp.core.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.lexilabs.basic.ads.AdState
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.rememberRewardedAd
import io.github.aakira.napier.Napier
import me.ltthuc.kmp.core.model.AppConfig
import org.koin.compose.koinInject

/**
 * Loads + shows a rewarded ad. Calls [onRewardEarned] when user finishes watching the ad
 * (and qualifies for the reward). Calls [onUnavailable] if ad fails to load OR user dismisses
 * before earning reward.
 *
 * Caller is responsible for showing this composable conditionally (e.g. when user opts in).
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun RewardedSkipLauncher(
    onRewardEarned: () -> Unit,
    onUnavailable: () -> Unit,
) {
    val appConfig: AppConfig = koinInject()
    val ad by rememberRewardedAd(
        adUnitId = appConfig.adMobRewardedAdUnitId,
        onFailure = { e ->
            Napier.w(tag = "RewardedSkipLauncher") { "Load fail: ${e.message}" }
            onUnavailable()
        },
    )
    var triggered by remember { mutableStateOf(false) }
    var rewarded by remember { mutableStateOf(false) }
    LaunchedEffect(ad.state) {
        if (!triggered && ad.state == AdState.READY) {
            triggered = true
            ad.setListeners(
                onFailure = { e ->
                    Napier.w(tag = "RewardedSkipLauncher") { "Show fail: ${e.message}" }
                    onUnavailable()
                },
                onDismissed = { if (!rewarded) onUnavailable() },
            )
            ad.show(
                onRewardEarned = {
                    rewarded = true
                    onRewardEarned()
                },
            )
        }
    }
}
