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
import me.ltthuc.kmp.core.model.Language
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.language_english
import me.ltthuc.kmp.core.resource.language_system
import me.ltthuc.kmp.core.resource.language_vietnamese
import me.ltthuc.kmp.core.resource.setting_general
import me.ltthuc.kmp.core.resource.setting_language
import me.ltthuc.kmp.core.resource.setting_language_description
import me.ltthuc.kmp.core.ui.screen.view.SegmentedTabRow
import me.ltthuc.kmp.feature.setting.components.SettingCard
import me.ltthuc.kmp.feature.setting.components.SettingTextItem
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingGeneralSection(
    language: Language,
    onLanguageChanged: (Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_general,
        )

        SettingCard {
            val languages = Language.entries
            var currentIndex by remember(language) { mutableStateOf(languages.indexOf(language)) }
            SettingTextItem(
                modifier = Modifier.fillMaxWidth(),
                title = Res.string.setting_language,
                description = Res.string.setting_language_description,
                onClick = null,
            )
            SegmentedTabRow(
                modifier = Modifier
                    .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                    .fillMaxWidth(),
                items = languages.toImmutableList(),
                selectedIndex = currentIndex,
                onSelect = {
                    currentIndex = it
                    onLanguageChanged(languages[it])
                },
                itemContent = @Composable { item, _ ->
                    Text(
                        text = when (item) {
                            Language.System -> stringResource(Res.string.language_system)
                            Language.English -> stringResource(Res.string.language_english)
                            Language.Vietnamese -> stringResource(Res.string.language_vietnamese)
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
            )
        }
    }
}
