package me.ltthuc.kmp.core.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lexilabs.basic.ads.AdSize
import app.lexilabs.basic.ads.DependsOnGoogleMobileAds
import app.lexilabs.basic.ads.composable.BannerAd
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.repository.AppSettingRepository
import org.koin.compose.koinInject

/**
 * Standard 320x50 banner ad pinned to bottom of screen content. Reads adUnitId from
 * Koin-injected [AppConfig] (test ID by default; production override via local.properties).
 *
 * Reserves vertical space (52dp) even before ad loads so layout doesn't jump.
 * Premium users (`hasPrivilege`) see no ads.
 */
@OptIn(DependsOnGoogleMobileAds::class)
@Composable
fun BottomBannerAd(modifier: Modifier = Modifier) {
    val appSettingRepository: AppSettingRepository = koinInject()
    val setting by appSettingRepository.setting.collectAsStateWithLifecycle()
    if (setting.hasPrivilege) return

    val appConfig: AppConfig = koinInject()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BANNER_HEIGHT_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        BannerAd(
            adUnitId = appConfig.adMobBannerAdUnitId,
            adSize = AdSize.LARGE_BANNER,
        )
    }
}

private const val BANNER_HEIGHT_DP = 102
