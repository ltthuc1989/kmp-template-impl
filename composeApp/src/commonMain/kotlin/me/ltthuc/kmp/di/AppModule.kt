package me.ltthuc.kmp.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import me.ltthuc.kmp.BuildKonfig
import me.ltthuc.kmp.core.audio.di.AudioConfig
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.model.Platform
import me.ltthuc.kmp.core.model.currentPlatform
import org.koin.core.module.Module
import org.koin.dsl.module

val appModule = module {
    single<CoroutineDispatcher> {
        Dispatchers.IO.limitedParallelism(24)
    }

    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>())
    }

    single {
        val adMobAppId: String
        val adMobBannerAdUnitId: String
        val adMobInterstitialAdUnitId: String
        val adMobRewardedAdUnitId: String
        val adMobAppOpenAdUnitId: String

        when (currentPlatform) {
            Platform.Android -> {
                adMobAppId = BuildKonfig.ADMOB_ANDROID_APP_ID
                adMobBannerAdUnitId = BuildKonfig.ADMOB_ANDROID_BANNER_AD_UNIT_ID
                adMobInterstitialAdUnitId = BuildKonfig.ADMOB_ANDROID_INTERSTITIAL_AD_UNIT_ID
                adMobRewardedAdUnitId = BuildKonfig.ADMOB_ANDROID_REWARDED_AD_UNIT_ID
                adMobAppOpenAdUnitId = BuildKonfig.ADMOB_ANDROID_APP_OPEN_AD_UNIT_ID
            }

            Platform.IOS -> {
                adMobAppId = BuildKonfig.ADMOB_IOS_APP_ID
                adMobBannerAdUnitId = BuildKonfig.ADMOB_IOS_BANNER_AD_UNIT_ID
                adMobInterstitialAdUnitId = BuildKonfig.ADMOB_IOS_INTERSTITIAL_AD_UNIT_ID
                adMobRewardedAdUnitId = BuildKonfig.ADMOB_IOS_REWARDED_AD_UNIT_ID
                adMobAppOpenAdUnitId = BuildKonfig.ADMOB_IOS_APP_OPEN_AD_UNIT_ID
            }
        }

        AppConfig(
            versionName = BuildKonfig.VERSION_NAME,
            versionCode = BuildKonfig.VERSION_CODE.toInt(),
            developerPin = BuildKonfig.DEVELOPER_PIN,
            purchaseAndroidApiKey = BuildKonfig.PURCHASE_ANDROID_API_KEY.takeIf { it.isNotBlank() },
            purchaseIosApiKey = BuildKonfig.PURCHASE_IOS_API_KEY.takeIf { it.isNotBlank() },
            adMobAppId = adMobAppId,
            adMobBannerAdUnitId = adMobBannerAdUnitId,
            adMobInterstitialAdUnitId = adMobInterstitialAdUnitId,
            adMobRewardedAdUnitId = adMobRewardedAdUnitId,
            adMobAppOpenAdUnitId = adMobAppOpenAdUnitId,
        )
    }

    single { AudioConfig(storageBucket = BuildKonfig.FIREBASE_STORAGE_BUCKET) }

    includes(appModulePlatform)
}

internal expect val appModulePlatform: Module
