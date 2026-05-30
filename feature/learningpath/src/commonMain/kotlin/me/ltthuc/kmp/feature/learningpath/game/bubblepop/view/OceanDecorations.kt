package me.ltthuc.kmp.feature.learningpath.game.bubblepop.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Decorative underwater scene rendered BEHIND the bubbles: 3 silhouette fish slowly
 * swaying left/right + a handful of tiny ambient bubbles drifting upward + a couple of
 * faint horizontal "current" lines. Purely visual — no interaction, doesn't block taps.
 *
 * Renders inside a fillMaxSize Canvas; the parent (BubblePopScreen) stacks it under
 * the BubbleCanvas via Box children order.
 */
@Composable
internal fun OceanDecorations(modifier: Modifier = Modifier) {
    val fish = remember {
        listOf(
            FishSpec(xFraction = 0.18f, yFraction = 0.28f, sizeDp = 56f, facingRight = true, phase = 0.0f),
            FishSpec(xFraction = 0.72f, yFraction = 0.52f, sizeDp = 80f, facingRight = false, phase = 0.35f),
            FishSpec(xFraction = 0.42f, yFraction = 0.78f, sizeDp = 48f, facingRight = true, phase = 0.65f),
        )
    }
    val ambientBubbles = remember {
        List(AMBIENT_BUBBLE_COUNT) {
            AmbientBubbleSpec(
                xFraction = Random.nextFloat(),
                phaseOffset = Random.nextFloat(),
                sizeDp = 4f + Random.nextFloat() * 8f,
                alpha = 0.12f + Random.nextFloat() * 0.18f,
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ocean")
    val fishSway by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = FISH_SWAY_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fishSway",
    )
    val ambientPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = AMBIENT_CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ambient",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCurrentLines(w, h)

        ambientBubbles.forEach { spec ->
            val localPhase = (ambientPhase + spec.phaseOffset) % 1f
            val baseX = spec.xFraction * w +
                sin(localPhase * 2f * PI.toFloat()) * 14f
            val y = h - localPhase * (h + 80f)
            drawCircle(
                color = Color.White.copy(alpha = spec.alpha * (1f - localPhase * 0.4f)),
                radius = spec.sizeDp * density,
                center = Offset(baseX, y),
                style = Stroke(width = 1.2f),
            )
        }

        fish.forEach { spec ->
            val swayX = sin((fishSway + spec.phase) * 2f * PI.toFloat()) * 36f
            val swayY = sin((fishSway + spec.phase) * 4f * PI.toFloat()) * 6f
            val cx = spec.xFraction * w + swayX
            val cy = spec.yFraction * h + swayY
            drawFish(
                center = Offset(cx, cy),
                widthPx = spec.sizeDp * density,
                facingRight = spec.facingRight,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCurrentLines(w: Float, h: Float) {
    val color = Color.White.copy(alpha = 0.08f)
    val effect = PathEffect.dashPathEffect(floatArrayOf(14f, 18f))
    drawLine(
        color = color,
        start = Offset(0f, h * 0.35f),
        end = Offset(w, h * 0.32f),
        strokeWidth = 1.5f,
        pathEffect = effect,
    )
    drawLine(
        color = color,
        start = Offset(0f, h * 0.66f),
        end = Offset(w, h * 0.69f),
        strokeWidth = 1.5f,
        pathEffect = effect,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFish(
    center: Offset,
    widthPx: Float,
    facingRight: Boolean,
) {
    val bodyW = widthPx
    val bodyH = widthPx * 0.55f
    val tailW = widthPx * 0.30f
    val dir = if (facingRight) 1f else -1f

    val bodyRect = Rect(
        left = center.x - bodyW / 2f,
        top = center.y - bodyH / 2f,
        right = center.x + bodyW / 2f,
        bottom = center.y + bodyH / 2f,
    )

    val bodyColor = Color(0xFF0A4E58).copy(alpha = 0.55f)
    val bellyColor = Color.White.copy(alpha = 0.18f)
    val eyeColor = Color.White.copy(alpha = 0.85f)

    // Body — ellipse
    drawOval(
        color = bodyColor,
        topLeft = Offset(bodyRect.left, bodyRect.top),
        size = Size(bodyRect.width, bodyRect.height),
    )
    // Belly highlight
    drawOval(
        color = bellyColor,
        topLeft = Offset(bodyRect.left + bodyW * 0.10f, bodyRect.top + bodyH * 0.40f),
        size = Size(bodyW * 0.80f, bodyH * 0.45f),
    )

    // Tail — triangle behind the body
    val tailBaseX = center.x - dir * (bodyW / 2f - bodyW * 0.05f)
    val tailTipX = tailBaseX - dir * tailW
    val path = Path().apply {
        moveTo(tailBaseX, center.y - bodyH * 0.35f)
        lineTo(tailTipX, center.y - bodyH * 0.55f)
        lineTo(tailTipX, center.y + bodyH * 0.55f)
        lineTo(tailBaseX, center.y + bodyH * 0.35f)
        close()
    }
    drawPath(path = path, color = bodyColor)

    // Eye
    val eyeX = center.x + dir * bodyW * 0.28f
    val eyeY = center.y - bodyH * 0.10f
    drawCircle(color = eyeColor, radius = bodyH * 0.08f, center = Offset(eyeX, eyeY))
    drawCircle(color = Color.Black.copy(alpha = 0.85f), radius = bodyH * 0.04f, center = Offset(eyeX, eyeY))
}

private data class FishSpec(
    val xFraction: Float,
    val yFraction: Float,
    val sizeDp: Float,
    val facingRight: Boolean,
    val phase: Float,
)

private data class AmbientBubbleSpec(
    val xFraction: Float,
    val phaseOffset: Float,
    val sizeDp: Float,
    val alpha: Float,
)

private const val FISH_SWAY_CYCLE_MS = 9_000
private const val AMBIENT_CYCLE_MS = 6_500
private const val AMBIENT_BUBBLE_COUNT = 14
private const val density = 1.5f
