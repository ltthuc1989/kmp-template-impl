package me.matsumo.grabee.feature.learningpath.step.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

/**
 * Renders expanding ripple rings when [isActive] is true. Reusable for Listen button
 * (sound wave), Mic button (recording indicator), and future animations.
 *
 * Place inside a Box with defined size — rings match parent size and scale outward.
 *
 * @param isActive when false, no rings render (composable returns early)
 * @param ringColor base color of rings (alpha is animated)
 * @param ringCount number of concurrent rings (default 3 — staggered phase)
 * @param maxScale how far rings grow beyond parent (1.8f = 180% of parent size)
 * @param durationMs total duration for one ring cycle
 */
@Composable
internal fun BoxScope.PulseRings(
    isActive: Boolean,
    ringColor: Color,
    ringCount: Int = 3,
    maxScale: Float = 1.9f,
    durationMs: Int = 1500,
) {
    if (!isActive) return
    val transition = rememberInfiniteTransition(label = "pulse-rings")
    repeat(ringCount) { i ->
        val phaseOffset = (durationMs / ringCount) * i
        val scale by transition.animateFloat(
            initialValue = 1f,
            targetValue = maxScale,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, delayMillis = phaseOffset, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "scale-$i",
        )
        val alpha by transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMs, delayMillis = phaseOffset, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "alpha-$i",
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(ringColor),
        )
    }
}
