package me.ltthuc.kmp.feature.setting.components.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.StarRate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_support_feedback
import me.ltthuc.kmp.core.resource.setting_support_rate
import me.ltthuc.kmp.core.resource.setting_support_share
import me.ltthuc.kmp.core.resource.setting_support_title
import me.ltthuc.kmp.feature.setting.components.SettingCard
import me.ltthuc.kmp.feature.setting.components.SettingTitleItem
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SettingSupportSection(
    onFeedback: () -> Unit,
    onShare: () -> Unit,
    onRate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SettingTitleItem(
            modifier = Modifier.fillMaxWidth(),
            text = Res.string.setting_support_title,
        )

        SettingCard {
            SupportRow(
                icon = Icons.Outlined.Email,
                label = stringResource(Res.string.setting_support_feedback),
                onClick = onFeedback,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SupportRow(
                icon = Icons.Outlined.Share,
                label = stringResource(Res.string.setting_support_share),
                onClick = onShare,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SupportRow(
                icon = Icons.Outlined.StarRate,
                label = stringResource(Res.string.setting_support_rate),
                onClick = onRate,
            )
        }
    }
}

@Composable
private fun SupportRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
