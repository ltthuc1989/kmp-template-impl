package me.ltthuc.kmp.feature.learningpath.game.bubblepop.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.BubbleSpec
import kotlin.math.hypot
import kotlin.random.Random

/**
 * The play area. Owns physics state per bubble — positions, velocities, pop/shake
 * animations. The ViewModel-owned [bubbles] list defines what to render; whenever the
 * list identity changes (e.g. new round), runtime state is rebuilt from scratch.
 *
 * Behavior:
 * - Bubbles drift upward with mild horizontal jitter; bounce off left/right edges; recycle
 *   from top → bottom (so kids always have ~6 bubbles in view).
 * - Tap inside any bubble's radius + [TAP_PAD_DP] hit-pad: invokes [onBubbleTapped].
 *   On correct → pop + burst particles, bubble removed.
 *   On wrong → shake animation; bubble stays.
 * - [speedMultiplier] = 1.0 for normal play; the dev preview uses 0.3 for slow-mo.
 */
@Composable
internal fun BubbleCanvas(
    bubbles: ImmutableList<BubbleSpec>,
    onBubbleTapped: (spec: BubbleSpec, isCorrect: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    speedMultiplier: Float = 1.0f,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val bubbleRadiusPx = with(density) { (BUBBLE_DIAMETER_DP / 2f).dp.toPx() }
        val tapPadPx = with(density) { TAP_PAD_DP.dp.toPx() }

        val scope = rememberCoroutineScope()

        val runtimes = remember(bubbles, widthPx, heightPx) {
            initRuntimes(bubbles, widthPx, heightPx, bubbleRadiusPx)
        }

        // Physics loop — drift up + bounce horizontally + recycle when off-top.
        LaunchedEffect(runtimes, widthPx, heightPx, speedMultiplier) {
            var lastNanos = 0L
            while (true) {
                withFrameNanos { now ->
                    val dt = if (lastNanos == 0L) {
                        0.016f
                    } else {
                        ((now - lastNanos) / 1_000_000_000f)
                            .coerceAtMost(0.05f) // clamp dt to avoid jumps after pause
                    }
                    lastNanos = now
                    val scaledDt = dt * speedMultiplier
                    runtimes.forEach { rt ->
                        if (!rt.isAlive) return@forEach
                        var pos = rt.position
                        var vel = rt.velocity
                        pos = Offset(pos.x + vel.x * scaledDt, pos.y + vel.y * scaledDt)

                        if (pos.x < bubbleRadiusPx) {
                            pos = pos.copy(x = bubbleRadiusPx)
                            vel = vel.copy(x = -vel.x)
                        } else if (pos.x > widthPx - bubbleRadiusPx) {
                            pos = pos.copy(x = widthPx - bubbleRadiusPx)
                            vel = vel.copy(x = -vel.x)
                        }

                        if (pos.y < -bubbleRadiusPx) {
                            // Bubble escaped top — distractors regen forever, targets retire.
                            if (rt.spec.isTarget) {
                                rt.isAlive = false
                                rt.visible.value = false
                                return@forEach
                            } else {
                                pos = Offset(
                                    x = bubbleRadiusPx + Random.nextFloat() *
                                        (widthPx - 2 * bubbleRadiusPx).coerceAtLeast(0f),
                                    y = heightPx + bubbleRadiusPx,
                                )
                                vel = vel.copy(x = randomJitter())
                            }
                        }

                        rt.position = pos
                        rt.velocity = vel
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(runtimes) {
                    detectTapGestures(
                        onPress = { offset ->
                            handleTap(
                                tapPos = offset,
                                runtimes = runtimes,
                                widthPx = widthPx,
                                heightPx = heightPx,
                                radiusPx = bubbleRadiusPx,
                                tapPadPx = tapPadPx,
                                scope = scope,
                                onBubbleTapped = onBubbleTapped,
                            )
                        },
                    )
                },
        ) {
            runtimes.forEach { rt ->
                if (!rt.visible.value) return@forEach
                val pos = rt.position
                val tint = bubbleTintFor(rt.spec.id)
                BubbleView(
                    letter = rt.spec.letter,
                    modifier = Modifier
                        .positionedAt(pos, bubbleRadiusPx)
                        .scale(rt.scale.value)
                        .alpha(rt.alpha.value)
                        .rotate(rt.rotation.value),
                    tint = tint,
                )
                if (rt.showBurst.value) {
                    PopBurst(
                        color = Color.White,
                        modifier = Modifier.positionedAt(pos, bubbleRadiusPx),
                    )
                }
            }
        }
    }
}

/** Positions a fixed-size child so its center sits at [center] in the parent's local pixels. */
private fun Modifier.positionedAt(center: Offset, radiusPx: Float): Modifier = this.layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height) {
        placeable.place(
            x = (center.x - radiusPx).toInt(),
            y = (center.y - radiusPx).toInt(),
        )
    }
}

/**
 * Per-bubble runtime state. Position + velocity update every frame; scale/alpha/rotation
 * are Animatables driven by pop/shake coroutines.
 */
internal class BubbleRuntime(val spec: BubbleSpec) {
    var position by mutableStateOf(Offset.Zero)
    var velocity by mutableStateOf(Offset.Zero)
    var isAlive: Boolean = true
    val visible = mutableStateOf(true)
    val scale = Animatable(1f)
    val alpha = Animatable(1f)
    val rotation = Animatable(0f)
    val showBurst = mutableStateOf(false)
    var isAnimating: Boolean = false
}

private fun initRuntimes(
    bubbles: ImmutableList<BubbleSpec>,
    widthPx: Float,
    heightPx: Float,
    radiusPx: Float,
): SnapshotStateList<BubbleRuntime> {
    val list = mutableStateListOf<BubbleRuntime>()
    if (widthPx <= 0f || heightPx <= 0f) return list
    val usableW = (widthPx - 2 * radiusPx).coerceAtLeast(0f)
    // Distinct stagger per bubble TYPE so targets get distributed over ~20s while distractors
    // come in quickly (first ~2s). Math: target stagger = TARGET_STAGGER_PX (~162px) per target
    // index. With min drift 81 px/s → last target enters visible at ~(10 × 162 / 81) ≈ 20s.
    var targetIdx = 0
    var distractorIdx = 0
    bubbles.forEach { spec ->
        val rt = BubbleRuntime(spec)
        val staggerOffset = if (spec.isTarget) {
            val offset = targetIdx * TARGET_STAGGER_PX
            targetIdx += 1
            offset
        } else {
            val offset = distractorIdx * radiusPx * 0.8f
            distractorIdx += 1
            offset
        }
        rt.position = Offset(
            x = radiusPx + Random.nextFloat() * usableW,
            y = heightPx + radiusPx + staggerOffset + Random.nextFloat() * 20f,
        )
        val upwardSpeed = -(
            BASE_UPWARD_PX_PER_S_MIN +
                Random.nextFloat() * (BASE_UPWARD_PX_PER_S_MAX - BASE_UPWARD_PX_PER_S_MIN)
            )
        val sideSpeed = randomJitter()
        rt.velocity = Offset(sideSpeed, upwardSpeed)
        list.add(rt)
    }
    return list
}

private fun randomJitter(): Float =
    (Random.nextFloat() - 0.5f) * 2f * JITTER_PX_PER_S_MAX

private fun handleTap(
    tapPos: Offset,
    runtimes: List<BubbleRuntime>,
    widthPx: Float,
    heightPx: Float,
    radiusPx: Float,
    tapPadPx: Float,
    scope: CoroutineScope,
    onBubbleTapped: (BubbleSpec, Boolean) -> Unit,
) {
    val hitRadius = radiusPx + tapPadPx
    val hit = runtimes
        .filter { it.isAlive && !it.isAnimating }
        .map { it to hypot(it.position.x - tapPos.x, it.position.y - tapPos.y) }
        .filter { (_, dist) -> dist <= hitRadius }
        .minByOrNull { it.second }
        ?.first ?: return
    if (hit.spec.isTarget) {
        animatePopAndRespawn(scope, hit, widthPx, heightPx, radiusPx)
        onBubbleTapped(hit.spec, true)
    } else {
        animateShake(scope, hit)
        onBubbleTapped(hit.spec, false)
    }
}

/**
 * Pop animation. Distractors: respawn at bottom (regen forever). Targets: mark dead
 * permanently — all 10 target spawns are pre-seeded at round init with staggered Y, no
 * respawn needed. Round ends when timer expires OR popCount=10.
 */
private fun animatePopAndRespawn(
    scope: CoroutineScope,
    rt: BubbleRuntime,
    widthPx: Float,
    heightPx: Float,
    radiusPx: Float,
) {
    rt.isAnimating = true
    scope.launch {
        rt.showBurst.value = true
        val scaleJob = launch {
            rt.scale.animateTo(1.25f, animationSpec = tween(80))
            rt.scale.animateTo(0f, animationSpec = tween(220))
        }
        val alphaJob = launch { rt.alpha.animateTo(0f, animationSpec = tween(POP_DURATION_MS)) }
        scaleJob.join()
        alphaJob.join()
        rt.showBurst.value = false

        if (rt.spec.isTarget) {
            rt.isAlive = false
            rt.visible.value = false
        } else {
            // distractors always regenerate at bottom
            rt.position = Offset(
                x = radiusPx + Random.nextFloat() *
                    (widthPx - 2 * radiusPx).coerceAtLeast(0f),
                y = heightPx + radiusPx,
            )
            val upwardSpeed = -(BASE_UPWARD_PX_PER_S_MIN +
                Random.nextFloat() * (BASE_UPWARD_PX_PER_S_MAX - BASE_UPWARD_PX_PER_S_MIN))
            rt.velocity = Offset(randomJitter(), upwardSpeed)
            rt.scale.snapTo(1f)
            rt.alpha.snapTo(1f)
        }
        rt.isAnimating = false
    }
}

private fun animateShake(scope: CoroutineScope, rt: BubbleRuntime) {
    rt.isAnimating = true
    scope.launch {
        rt.rotation.snapTo(0f)
        rt.rotation.animateTo(-SHAKE_ANGLE, animationSpec = tween(60))
        rt.rotation.animateTo(SHAKE_ANGLE, animationSpec = tween(90))
        rt.rotation.animateTo(-SHAKE_ANGLE * 0.6f, animationSpec = tween(80))
        rt.rotation.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
        delay(40)
        rt.isAnimating = false
    }
}

// Drift speed: cumulative +20% (v5) + +30% (v5b) + +30% (v5c) vs original — total ~2x faster
private const val BASE_UPWARD_PX_PER_S_MIN = 81f
private const val BASE_UPWARD_PX_PER_S_MAX = 152f
private const val JITTER_PX_PER_S_MAX = 30f
private const val SHAKE_ANGLE = 9f
private const val TAP_PAD_DP = 8

// Target stagger: ~162px per target index ⇒ at min drift (81 px/s), bubbles enter visible
// 2s apart. 10 targets → all 10 visible within ~20s of round start, well under the 25s mark.
private const val TARGET_STAGGER_PX = 162f
