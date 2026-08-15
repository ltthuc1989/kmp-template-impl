package me.ltthuc.kmp.feature.learningpath.game.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * How long a screen waits with no user action before showing its idle guide hand. Shared so every
 * mini-game (and any other idle hint) uses the same beat.
 */
const val GUIDE_IDLE_MS = 10_000L

/** One demo gesture for [GameHandGuide]. */
sealed interface HandStep {
    /** Tap (press) at [pos]. */
    data class Tap(val pos: Offset) : HandStep

    /** Grab at [from], drag to [to], release (a trail line is drawn). */
    data class Drag(val from: Offset, val to: Offset) : HandStep
}

/**
 * Idle tutorial overlay: a default 👆 emoji hand that mimes the game's gesture(s) — tap (press +
 * ripple) and/or drag (grab → move with a trail → release). Loops through [steps] until the parent
 * hides it ([isVisible] = false on first interaction). Positions are in the hosting Box's
 * coordinate space (px). Reusable across games.
 */
@Composable
fun BoxScope.GameHandGuide(
    isVisible: Boolean,
    steps: ImmutableList<HandStep>,
) {
    if (!isVisible || steps.isEmpty()) return
    val density = LocalDensity.current
    val handPx = with(density) { HAND_SIZE_DP.dp.toPx() }

    val alpha = remember { Animatable(0f) }
    val press = remember { Animatable(0f) } // 0 = up, 1 = pressed
    val ripple = remember { Animatable(0f) }
    var handPos by remember { mutableStateOf(steps.first().start()) }
    var dragFrom by remember { mutableStateOf<Offset?>(null) }

    suspend fun rippleOnce() {
        ripple.snapTo(0f)
        ripple.animateTo(1f, tween(RIPPLE_MS))
    }

    suspend fun tapAt(pos: Offset) = coroutineScope {
        handPos = pos
        dragFrom = null
        launch { rippleOnce() }
        press.animateTo(1f, tween(PRESS_DOWN_MS))
        press.animateTo(0f, tween(PRESS_UP_MS))
    }

    suspend fun dragFromTo(from: Offset, to: Offset) {
        handPos = from
        dragFrom = from
        press.animateTo(1f, tween(PRESS_DOWN_MS)) // grab
        val move = Animatable(0f)
        move.animateTo(1f, tween(DRAG_MS, easing = FastOutSlowInEasing)) {
            handPos = lerp(from, to, value)
        }
        press.animateTo(0f, tween(PRESS_UP_MS)) // release
        dragFrom = null
    }

    LaunchedEffect(isVisible, steps) {
        alpha.snapTo(0f)
        press.snapTo(0f)
        ripple.snapTo(0f)
        dragFrom = null
        while (true) {
            handPos = steps.first().start()
            alpha.animateTo(1f, tween(FADE_MS))
            for (step in steps) {
                when (step) {
                    is HandStep.Tap -> tapAt(step.pos)
                    is HandStep.Drag -> dragFromTo(step.from, step.to)
                }
            }
            alpha.animateTo(0f, tween(FADE_MS))
            delay(LOOP_GAP_MS)
        }
    }

    // Drag trail + tap ripple.
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
    Canvas(modifier = Modifier.matchParentSize().alpha(alpha.value)) {
        dragFrom?.let { from ->
            drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = from,
                end = handPos,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dash,
            )
        }
        if (ripple.value > 0f) {
            drawCircle(
                color = Color.White.copy(alpha = (1f - ripple.value) * 0.45f),
                radius = (8f + ripple.value * 26f).dp.toPx(),
                center = handPos,
            )
        }
    }

    // 👆 anchored so the fingertip sits on handPos; presses (scale + dip) around the fingertip.
    val offX = with(density) { (handPos.x - TIP_FX * handPx).toDp() }
    val offY = with(density) { (handPos.y - TIP_FY * handPx).toDp() } + (press.value * PRESS_DP).dp
    val scale = 1f - press.value * PRESS_SCALE
    Box(
        modifier = Modifier
            .offset(x = offX, y = offY)
            .size(HAND_SIZE_DP.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(TIP_FX, TIP_FY)
                this.alpha = alpha.value
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "👆", fontSize = HAND_SP.sp)
    }
}

private fun HandStep.start(): Offset = when (this) {
    is HandStep.Tap -> pos
    is HandStep.Drag -> from
}

private const val HAND_SIZE_DP = 52
private const val HAND_SP = 40
private const val TIP_FX = 0.5f
private const val TIP_FY = 0.18f
private const val PRESS_DP = 6f
private const val PRESS_SCALE = 0.12f
private const val FADE_MS = 220
private const val PRESS_DOWN_MS = 150
private const val PRESS_UP_MS = 220
private const val RIPPLE_MS = 430
private const val DRAG_MS = 850
private const val LOOP_GAP_MS = 600L
