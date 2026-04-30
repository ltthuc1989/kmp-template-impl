package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_paywall_description
import me.ltthuc.kmp.core.resource.setting_paywall_title
import me.ltthuc.kmp.core.resource.setting_paywall_upgrade
import me.ltthuc.kmp.feature.setting.components.SettingTextItem
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem

@Composable
internal fun SettingPaywallSection(
    modifier: Modifier = Modifier,
    onUpgradeClicked: () -> Unit,
) {
    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_paywall_title,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_paywall_upgrade,
            description = Res.string.setting_paywall_description,
            onClick = onUpgradeClicked,
        )
    }
}
