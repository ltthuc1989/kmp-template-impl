package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.level_name
import me.ltthuc.kmp.core.resource.paywall_restore
import me.ltthuc.kmp.core.resource.setting_parent_control_buy
import me.ltthuc.kmp.core.resource.setting_parent_control_buy_plain
import me.ltthuc.kmp.core.resource.setting_parent_control_not_owned_label
import me.ltthuc.kmp.core.resource.setting_parent_control_title
import me.ltthuc.kmp.core.resource.setting_unlocked_label
import me.ltthuc.kmp.feature.setting.components.SettingCard
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

/**
 * Premium section: shows each sellable level's purchase state. A not-yet-owned level offers the buy
 * action (→ paywall); an owned level just shows "Unlocked" (one-time purchase — never re-locked).
 * A Restore action recovers purchases on a new device / reinstall (store-account based).
 */
@Composable
internal fun SettingParentControlSection(
    levels: ImmutableList<Level>,
    ownedLevelIds: ImmutableSet<String>,
    levelPrices: ImmutableMap<String, String>,
    onBuy: (levelId: String) -> Unit,
    onRestore: () -> Unit,
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
                LevelPurchaseRow(
                    level = level,
                    owned = level.id in ownedLevelIds,
                    price = levelPrices[level.id],
                    onBuy = { onBuy(level.id) },
                )
            }
        }
        TextButton(
            onClick = onRestore,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text(stringResource(Res.string.paywall_restore))
        }
    }
}

@Composable
private fun LevelPurchaseRow(
    level: Level,
    owned: Boolean,
    price: String?,
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
                text = stringResource(Res.string.level_name, level.number, level.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (owned) {
                    stringResource(Res.string.setting_unlocked_label)
                } else {
                    stringResource(Res.string.setting_parent_control_not_owned_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (owned) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            TextButton(onClick = onBuy) {
                Text(
                    if (price != null) {
                        stringResource(Res.string.setting_parent_control_buy, price)
                    } else {
                        stringResource(Res.string.setting_parent_control_buy_plain)
                    },
                )
            }
        }
    }
}
