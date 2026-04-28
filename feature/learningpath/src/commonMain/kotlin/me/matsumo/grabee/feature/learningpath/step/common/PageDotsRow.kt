package me.matsumo.grabee.feature.learningpath.step.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@Composable
internal fun PageDotsRow(
    currentPage: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    if (total <= 1) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(total) { index ->
            val isActive = index == currentPage
            val dotColor = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val elevation = if (isActive) 2.dp else 0.dp
            Box(
                modifier = Modifier
                    .size(DOT_SIZE_DP.dp)
                    .shadow(elevation = elevation, shape = CircleShape)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
    }
}

private const val DOT_SIZE_DP = 10
