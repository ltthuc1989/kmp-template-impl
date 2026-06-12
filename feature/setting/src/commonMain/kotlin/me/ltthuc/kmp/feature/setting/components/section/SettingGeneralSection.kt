package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_general
import me.ltthuc.kmp.core.resource.setting_sound_effects
import me.ltthuc.kmp.feature.setting.components.SettingCard
import me.ltthuc.kmp.feature.setting.components.SettingSwitchItem
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem

@Composable
internal fun SettingGeneralSection(
    sfxEnabled: Boolean,
    onSfxEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_general,
        )

        SettingCard {
            SettingSwitchItem(
                modifier = Modifier.fillMaxWidth(),
                title = Res.string.setting_sound_effects,
                description = null,
                value = sfxEnabled,
                onValueChanged = onSfxEnabledChanged,
            )
        }
    }
}
