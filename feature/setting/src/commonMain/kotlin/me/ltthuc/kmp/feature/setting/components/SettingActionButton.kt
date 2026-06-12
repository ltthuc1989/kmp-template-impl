package me.ltthuc.kmp.feature.setting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.ui.theme.bold

/**
 * Full-width bordered card acting as a button with a centered, bold colored label.
 * Used for actions like Reset Progress ([color] = `colorScheme.error` for destructive).
 */
@Composable
internal fun SettingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall.bold(),
            color = color,
        )
    }
}
