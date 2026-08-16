package me.ltthuc.kmp.feature.billing.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.paywall_feature_high_quality
import me.ltthuc.kmp.core.resource.paywall_feature_high_quality_desc
import me.ltthuc.kmp.core.resource.paywall_feature_unlimited_downloads
import me.ltthuc.kmp.core.resource.paywall_feature_unlimited_downloads_desc
import org.jetbrains.compose.resources.stringResource

/** [levelName] is the level being sold ("Level 2: Short Vowels"); null when no single level is. */
@Composable
internal fun PaywallFeatureList(
    levelName: String?,
    unitCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        // The paywall itself scrolls; a second vertical scroller inside it fights the
        // first and traps drags that start on a feature row.
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Named after the level being sold. These lines used to be fixed text that said
        // "Level 1" no matter which level the parent had tapped. Dropped entirely when there is no
        // single level, rather than naming an empty one.
        if (levelName != null) {
            PaywallFeatureItem(
                icon = Icons.Default.Download,
                title = stringResource(Res.string.paywall_feature_unlimited_downloads, levelName),
                description = stringResource(
                    Res.string.paywall_feature_unlimited_downloads_desc,
                    unitCount,
                    levelName,
                ),
            )
        }
        PaywallFeatureItem(
            icon = Icons.Default.HighQuality,
            title = stringResource(Res.string.paywall_feature_high_quality),
            description = stringResource(Res.string.paywall_feature_high_quality_desc),
        )
    }
}

@Composable
private fun PaywallFeatureItem(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
