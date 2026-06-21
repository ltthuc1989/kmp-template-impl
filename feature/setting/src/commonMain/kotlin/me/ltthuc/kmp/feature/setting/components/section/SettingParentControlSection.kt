package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_parent_control_buy
import me.ltthuc.kmp.core.resource.setting_parent_control_lock
import me.ltthuc.kmp.core.resource.setting_parent_control_not_owned_label
import me.ltthuc.kmp.core.resource.setting_parent_control_open
import me.ltthuc.kmp.core.resource.setting_parent_control_opened_label
import me.ltthuc.kmp.core.resource.setting_parent_control_sequential_label
import me.ltthuc.kmp.core.resource.setting_parent_control_title
import me.ltthuc.kmp.feature.setting.components.SettingCard
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

/**
 * Parent control to flip an OWNED level between the sequential learning gate and "all units open".
 * Never bypasses the paywall: a not-yet-purchased level only offers the buy action (→ paywall).
 */
@Composable
internal fun SettingParentControlSection(
    levels: ImmutableList<Level>,
    ownedLevelIds: ImmutableSet<String>,
    manualUnlockedLevelIds: ImmutableSet<String>,
    onOpenAll: (levelId: String) -> Unit,
    onLock: (levelId: String) -> Unit,
    onBuy: (levelId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_parent_control_title,
        )
        SettingCard {
            levels.forEachIndexed { index, level ->
                if (index > 0) HorizontalDivider()
                ParentControlRow(
                    level = level,
                    owned = level.id in ownedLevelIds,
                    openedFully = level.id in manualUnlockedLevelIds,
                    onOpenAll = { onOpenAll(level.id) },
                    onLock = { onLock(level.id) },
                    onBuy = { onBuy(level.id) },
                )
            }
        }
    }
}

@Composable
private fun ParentControlRow(
    level: Level,
    owned: Boolean,
    openedFully: Boolean,
    onOpenAll: () -> Unit,
    onLock: () -> Unit,
    onBuy: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Level ${level.number} · ${level.title}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val statusLabel = when {
                !owned -> stringResource(Res.string.setting_parent_control_not_owned_label)
                openedFully -> stringResource(Res.string.setting_parent_control_opened_label)
                else -> stringResource(Res.string.setting_parent_control_sequential_label)
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            !owned -> TextButton(onClick = onBuy) {
                Text(stringResource(Res.string.setting_parent_control_buy))
            }
            openedFully -> OutlinedButton(onClick = onLock) {
                Text(stringResource(Res.string.setting_parent_control_lock))
            }
            else -> OutlinedButton(onClick = onOpenAll) {
                Text(stringResource(Res.string.setting_parent_control_open))
            }
        }
    }
}
