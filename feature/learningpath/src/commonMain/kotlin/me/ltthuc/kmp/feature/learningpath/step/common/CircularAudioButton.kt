package me.ltthuc.kmp.feature.learningpath.step.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Circular play/listen button with a soft pulse-ring animation when active.
 * Visual identity matches Story step's audio CTA so all step screens share the
 * same listen-button language.
 */
@Composable
internal fun CircularAudioButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = AUDIO_BUTTON_SIZE_DP.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val ringSize = size + 8.dp
    Box(
        modifier = modifier.size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        PulseRings(
            isActive = isPlaying,
            ringColor = primary.copy(alpha = RING_ALPHA),
        )
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(size)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(colors = listOf(primary, primaryContainer)),
                ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

internal const val AUDIO_BUTTON_SIZE_DP = 64
private const val RING_ALPHA = 0.35f
