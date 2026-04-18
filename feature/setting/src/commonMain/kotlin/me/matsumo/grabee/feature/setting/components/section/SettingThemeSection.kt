package me.matsumo.grabee.feature.setting.components.section

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
import me.matsumo.grabee.core.model.AppSetting
import me.matsumo.grabee.core.model.AppThemePalette
import me.matsumo.grabee.core.model.Theme
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.setting_palette
import me.matsumo.grabee.core.resource.setting_palette_description
import me.matsumo.grabee.core.resource.setting_palette_fluid
import me.matsumo.grabee.core.resource.setting_palette_playful
import me.matsumo.grabee.core.resource.setting_theme
import me.matsumo.grabee.core.resource.setting_theme_app
import me.matsumo.grabee.core.resource.setting_theme_app_auto
import me.matsumo.grabee.core.resource.setting_theme_app_dark
import me.matsumo.grabee.core.resource.setting_theme_app_description
import me.matsumo.grabee.core.resource.setting_theme_app_light
import me.matsumo.grabee.core.ui.screen.view.SegmentedTabRow
import me.matsumo.grabee.feature.setting.components.SettingTextItem
import me.matsumo.grabee.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingThemeSection(
    setting: AppSetting,
    onThemeChanged: (Theme) -> Unit,
    onPaletteChanged: (AppThemePalette) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = Theme.entries
    var currentThemeIndex by remember(setting) { mutableStateOf(themes.indexOf(setting.theme)) }

    val palettes = AppThemePalette.entries
    var currentPaletteIndex by remember(setting) {
        mutableStateOf(palettes.indexOf(setting.appThemePalette))
    }

    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_theme,
        )

        SettingTextItem(
            modifier = Modifier.fillMaxWidth(),
            title = Res.string.setting_theme_app,
            description = Res.string.setting_theme_app_description,
            onClick = null,
        )

        SegmentedTabRow(
            modifier = Modifier
                .padding(16.dp, 8.dp)
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
                .padding(16.dp, 8.dp)
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
