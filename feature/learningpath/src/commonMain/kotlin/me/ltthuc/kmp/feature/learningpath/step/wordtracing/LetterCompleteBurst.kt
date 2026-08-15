package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * One-shot celebration fired when a letter is fully traced. Renders [PARTICLE_COUNT] rounded green
 * diamonds radiating outward from the canvas centre and fading over [BURST_MS], matching the
 * word-tracing mockup. Fills its parent Box so callers stack it directly over [LetterTraceCanvas];
 * particles are seeded once per mount, so a fresh burst plays each time the overlay is (re)mounted.
 */
@Composable
internal fun LetterCompleteBurst(modifier: Modifier = Modifier) {
    val particles = remember {
        List(PARTICLE_COUNT) {
            BurstParticle(
                // Spread angles roughly evenly with jitter so it never looks like a fixed star.
                angle = ((it + Random.nextFloat()) / PARTICLE_COUNT) * 2f * PI.toFloat(),
                travelFraction = 0.6f + Random.nextFloat() * 0.4f,
                sizeFraction = 0.06f + Random.nextFloat() * 0.07f,
                rotation = Random.nextFloat() * 90f,
                color = if (Random.nextFloat() < 0.55f) BurstGreen else TraceHighlightGreen,
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(BURST_MS, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val phase = progress.value
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxTravel = size.minDimension * 0.62f
        // Ramp alpha in fast (first 20%) then fade to nothing.
        val alpha = if (phase < 0.2f) phase / 0.2f else (1f - phase).coerceIn(0f, 1f)
        particles.forEach { p ->
            val travel = maxTravel * p.travelFraction * (0.3f + phase * 0.7f)
            val pos = Offset(
                x = center.x + cos(p.angle) * travel,
                y = center.y + sin(p.angle) * travel,
            )
            // Diamonds shrink slightly as they fly out.
            val side = size.minDimension * p.sizeFraction * (1f - phase * 0.35f)
            rotate(degrees = 45f + p.rotation, pivot = pos) {
                drawRoundRect(
                    color = p.color.copy(alpha = p.color.alpha * alpha),
                    topLeft = Offset(pos.x - side / 2f, pos.y - side / 2f),
                    size = Size(side, side),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(side * 0.28f),
                )
            }
        }
    }
}

private data class BurstParticle(
    val angle: Float,
    val travelFraction: Float,
    val sizeFraction: Float,
    val rotation: Float,
    val color: Color,
)

/** Vivid mid-green from the mockup; pairs with the pale [TraceHighlightGreen] for variety. */
private val BurstGreen = Color(0xFF7DC242)

internal const val BURST_MS = 850
private const val PARTICLE_COUNT = 14
