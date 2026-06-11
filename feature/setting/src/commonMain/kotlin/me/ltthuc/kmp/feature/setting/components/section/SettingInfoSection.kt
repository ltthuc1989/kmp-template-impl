package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_information
import me.ltthuc.kmp.core.resource.setting_information_app_version
import me.ltthuc.kmp.core.ui.theme.LocalAppConfig
import me.ltthuc.kmp.feature.setting.components.SettingTextItem
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingInfoSection(
    setting: AppSetting,
    modifier: Modifier = Modifier,
) {
    val appConfig = LocalAppConfig.current
    val clipboardManager = LocalClipboardManager.current

    val appVersion = "${appConfig.versionName}:${appConfig.versionCode} " + when {
        setting.plusMode && setting.developerMode -> "[P+D]"
        setting.plusMode -> "[Plus]"
        setting.developerMode -> "[Dev]"
        else -> ""
    }

    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_information,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(Res.string.setting_information_app_version),
            description = appVersion,
            onClick = { },
            onLongClick = { clipboardManager.setText(AnnotatedString(appVersion)) },
        )
    }
}
