package me.ltthuc.kmp

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import me.ltthuc.kmp.ads.AppOpenAdManager
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.di.applyModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration
import org.koin.mp.KoinPlatform

@OptIn(KoinExperimentalAPI::class)
class GrabeeApplication : Application(), KoinStartup {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            // StrictMode.enableDefaults()
            Napier.base(DebugAntilog())
        }

        if (MONETIZATION_ENABLED) {
            val appConfig = KoinPlatform.getKoin().get<AppConfig>()
            val appSettingRepository = KoinPlatform.getKoin().get<AppSettingRepository>()
            AppOpenAdManager(this, appConfig.adMobAppOpenAdUnitId, appSettingRepository)
        }
    }

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@GrabeeApplication)
        androidLogger()
        applyModules()
    }
}
