package me.ltthuc.kmp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.BillingRepository
import me.ltthuc.kmp.core.repository.ContentPackRepository
import org.koin.compose.koinInject

@Suppress("FunctionNaming")
fun MainViewController() = ComposeUIViewController {
    val settingRepository = koinInject<AppSettingRepository>()
    val billingRepository = koinInject<BillingRepository>()
    val contentPackRepository = koinInject<ContentPackRepository>()
    val userData by settingRepository.setting.collectAsStateWithLifecycle(null)

    LaunchedEffect(Unit) {
        billingRepository.configure()
        settingRepository.initializeIdIfNeeded()
        // See MainViewModel on Android: same tidy-up after an update.
        contentPackRepository.cleanUpAfterUpdate()
    }

    userData?.let {
        GrabeeApp(
            modifier = Modifier.fillMaxSize(),
            setting = it,
        )
    }
}
