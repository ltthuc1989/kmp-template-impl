package me.ltthuc.kmp.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import me.ltthuc.kmp.BuildKonfig
import me.ltthuc.kmp.core.content.di.ContentConfig
import me.ltthuc.kmp.core.model.AppConfig
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
        AppConfig(
            versionName = BuildKonfig.VERSION_NAME,
            versionCode = BuildKonfig.VERSION_CODE.toInt(),
            developerPin = BuildKonfig.DEVELOPER_PIN,
            purchaseAndroidApiKey = BuildKonfig.PURCHASE_ANDROID_API_KEY.takeIf { it.isNotBlank() },
            purchaseIosApiKey = BuildKonfig.PURCHASE_IOS_API_KEY.takeIf { it.isNotBlank() },
        )
    }

    single { ContentConfig(cdnBaseUrl = BuildKonfig.CONTENT_CDN_BASE_URL) }

    includes(appModulePlatform)
}

internal expect val appModulePlatform: Module
