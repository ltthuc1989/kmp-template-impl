package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Theme
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_other
import me.ltthuc.kmp.core.resource.setting_other_developer_mode
import me.ltthuc.kmp.core.resource.setting_other_developer_mode_description
import me.ltthuc.kmp.core.resource.setting_other_open_source_license
import me.ltthuc.kmp.core.resource.setting_other_open_source_license_description
import me.ltthuc.kmp.core.resource.setting_palette
import me.ltthuc.kmp.core.resource.setting_palette_description
import me.ltthuc.kmp.core.resource.setting_palette_fluid
import me.ltthuc.kmp.core.resource.setting_palette_playful
import me.ltthuc.kmp.core.resource.setting_theme_app
import me.ltthuc.kmp.core.resource.setting_theme_app_auto
import me.ltthuc.kmp.core.resource.setting_theme_app_dark
import me.ltthuc.kmp.core.resource.setting_theme_app_description
import me.ltthuc.kmp.core.resource.setting_theme_app_light
import me.ltthuc.kmp.core.ui.screen.view.SegmentedTabRow
import me.ltthuc.kmp.feature.setting.components.SettingCard
import me.ltthuc.kmp.feature.setting.components.SettingDeveloperModeDialog
import me.ltthuc.kmp.feature.setting.components.SettingSwitchItem
import me.ltthuc.kmp.feature.setting.components.SettingTextItem
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingOthersSection(
    setting: AppSetting,
    onOpenSourceLicenseClicked: () -> Unit,
    onDeveloperModeChanged: (Boolean) -> Unit,
    onShowSpeakButtonChanged: (Boolean) -> Unit,
    onThemeChanged: (Theme) -> Unit,
    onPaletteChanged: (AppThemePalette) -> Unit,
    onLetterGuideDebugClicked: () -> Unit,
    onBubblePopPreviewClicked: () -> Unit,
    onMemoryMatchPreviewClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isShowDeveloperModeDialog by remember { mutableStateOf(false) }

    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_other,
        )

        SettingCard {
            SettingTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = Res.string.setting_other_open_source_license,
                description = Res.string.setting_other_open_source_license_description,
                onClick = { onOpenSourceLicenseClicked.invoke() },
            )

            SettingSwitchItem(
                modifier = Modifier.fillMaxWidth(),
                title = Res.string.setting_other_developer_mode,
                description = Res.string.setting_other_developer_mode_description,
                value = setting.developerMode,
                onValueChanged = {
                    if (it) {
                        isShowDeveloperModeDialog = true
                    } else {
                        onDeveloperModeChanged.invoke(false)
                    }
                },
            )

            if (setting.developerMode) {
                ThemeControls(
                    setting = setting,
                    onThemeChanged = onThemeChanged,
                    onPaletteChanged = onPaletteChanged,
                )
                SettingSwitchItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Show speak button",
                    description = "Dev: hide mic button when speak feature is not ready",
                    value = setting.showSpeakButton,
                    onValueChanged = onShowSpeakButtonChanged,
                )
                SettingTextItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Letter guide QA",
                    description = "Visual review of all 52 Zaner-Bloser tracing guides",
                    onClick = onLetterGuideDebugClicked,
                )
                SettingTextItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Bubble Pop Preview",
                    description = "Dev: test bubble physics + pop animation standalone",
                    onClick = onBubblePopPreviewClicked,
                )
                SettingTextItem(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Memory Match Preview",
                    description = "Dev: test card flip + match animations standalone",
                    onClick = onMemoryMatchPreviewClicked,
                )
            }
        }
    }

    if (isShowDeveloperModeDialog) {
        SettingDeveloperModeDialog(
            onDeveloperModeEnabled = {
                onDeveloperModeChanged.invoke(true)
                isShowDeveloperModeDialog = false
            },
            onDismissRequest = {
                isShowDeveloperModeDialog = false
            },
        )
    }
}

@Composable
private fun ThemeControls(
    setting: AppSetting,
    onThemeChanged: (Theme) -> Unit,
    onPaletteChanged: (AppThemePalette) -> Unit,
) {
    val themes = Theme.entries
    var currentThemeIndex by remember(setting) { mutableStateOf(themes.indexOf(setting.theme)) }

    val palettes = AppThemePalette.entries
    var currentPaletteIndex by remember(setting) {
        mutableStateOf(palettes.indexOf(setting.appThemePalette))
    }

    Column {
        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_theme_app,
            description = Res.string.setting_theme_app_description,
            onClick = null,
        )

        SegmentedTabRow(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                .fillMaxWidth(),
            items = themes.toImmutableList(),
            selectedIndex = currentThemeIndex,
            onSelect = {
                currentThemeIndex = it
                onThemeChanged.invoke(themes[it])
            },
            itemContent = @Composable { item, _ ->
                Text(
                    text = when (item) {
                        Theme.System -> stringResource(Res.string.setting_theme_app_auto)
                        Theme.Light -> stringResource(Res.string.setting_theme_app_light)
                        Theme.Dark -> stringResource(Res.string.setting_theme_app_dark)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            },
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_palette,
            description = Res.string.setting_palette_description,
            onClick = null,
        )

        SegmentedTabRow(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                .fillMaxWidth(),
            items = palettes.toImmutableList(),
            selectedIndex = currentPaletteIndex,
            onSelect = {
                currentPaletteIndex = it
                onPaletteChanged.invoke(palettes[it])
            },
            itemContent = @Composable { item, _ ->
                Text(
                    text = when (item) {
                        AppThemePalette.PlayfulMentor -> stringResource(Res.string.setting_palette_playful)
                        AppThemePalette.FluidArchitect -> stringResource(Res.string.setting_palette_fluid)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            },
        )
    }
}
