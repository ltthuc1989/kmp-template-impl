package me.ltthuc.kmp.feature.learningpath.game.bubblepop.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * One-shot particle burst rendered when a bubble pops. Six dots radiating outward, fading
 * to transparent over [POP_DURATION_MS]. Renders inside a Box of size [diameter] so callers
 * can stack it directly over the popped bubble's position.
 */
@Composable
internal fun PopBurst(
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = BUBBLE_DIAMETER_DP.dp,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(POP_DURATION_MS))
    }
    Box(modifier = modifier.size(diameter)) {
        Canvas(modifier = Modifier.size(diameter)) {
            val phase = progress.value
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val travel = radius * (0.4f + phase * 1.0f)
            val particleRadius = radius * 0.12f * (1f - phase)
            val alpha = (1f - phase).coerceIn(0f, 1f)
            repeat(PARTICLE_COUNT) { i ->
                val angle = (i.toFloat() / PARTICLE_COUNT) * 2f * kotlin.math.PI.toFloat()
                val px = center.x + cos(angle) * travel
                val py = center.y + sin(angle) * travel
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = particleRadius,
                    center = Offset(px, py),
                )
            }
        }
    }
}

internal const val POP_DURATION_MS = 320
private const val PARTICLE_COUNT = 6
