package me.ltthuc.kmp.feature.learningpath.step.matching

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.model.LessonWord

/**
 * Animated drag-to-connect tutorial overlay for the Matching step.
 *
 * Single hand pointer that alternates between two demos to teach kids that drag works
 * in either direction:
 *   - Phase 1: text[wordA] → image[wordA]   (e.g., "ant" → 🐜)
 *   - Phase 2: image[wordB] → text[wordB]   (e.g., 🍎 → "apple")
 * Different words each phase so the kid sees variety, not repetition.
 *
 * Loops indefinitely until the user touches the area (parent flips [isVisible] to false
 * via `hintDismissed`). If the lesson has only one word, both phases reuse it.
 */
@Suppress("UnstableCollections")
@Composable
internal fun BoxScope.MatchingDragHint(
    isVisible: Boolean,
    leftItems: ImmutableList<LessonWord>,
    rightItems: ImmutableList<LessonWord>,
    leftDotPositions: Map<String, Offset>,
    rightDotPositions: Map<String, Offset>,
) {
    val firstWord = leftItems.firstOrNull() ?: return
    val secondWord = leftItems.firstOrNull { it.word != firstWord.word } ?: firstWord
    val firstRight = rightItems.firstOrNull { it.word.equals(firstWord.word, ignoreCase = true) }
        ?: return
    val secondRight = rightItems.firstOrNull { it.word.equals(secondWord.word, ignoreCase = true) }
        ?: return
    val textDotA = leftDotPositions[firstWord.word] ?: return
    val imageDotA = rightDotPositions[firstRight.word] ?: return
    val textDotB = leftDotPositions[secondWord.word] ?: return
    val imageDotB = rightDotPositions[secondRight.word] ?: return
    if (!isVisible) return

    val progress = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    var directionIsLeftToRight by remember { mutableStateOf(true) }

    LaunchedEffect(isVisible, textDotA, imageDotA, textDotB, imageDotB) {
        while (true) {
            // Phase 1: text → image, word A
            directionIsLeftToRight = true
            progress.snapTo(0f)
            alpha.snapTo(0f)
            alpha.animateTo(1f, tween(FADE_MS))
            progress.animateTo(1f, tween(TRAVEL_MS, easing = FastOutSlowInEasing))
            delay(HOLD_AT_END_MS)
            alpha.animateTo(0f, tween(FADE_MS))
            delay(LOOP_GAP_MS)

            // Phase 2: image → text, word B (different word)
            directionIsLeftToRight = false
            progress.snapTo(0f)
            alpha.animateTo(1f, tween(FADE_MS))
            progress.animateTo(1f, tween(TRAVEL_MS, easing = FastOutSlowInEasing))
            delay(HOLD_AT_END_MS)
            alpha.animateTo(0f, tween(FADE_MS))
            delay(LOOP_GAP_MS)
        }
    }

    val (start, end) = if (directionIsLeftToRight) {
        textDotA to imageDotA
    } else {
        imageDotB to textDotB
    }
    val handPos = lerp(start, end, progress.value)
    val primary = MaterialTheme.colorScheme.primary
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize().alpha(alpha.value)) {
        drawLine(
            color = primary.copy(alpha = TRAIL_ALPHA),
            start = start,
            end = handPos,
            strokeWidth = TRAIL_STROKE_DP.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = dashEffect,
        )
    }

    val handOffsetX = with(density) { (handPos.x - HAND_HALF_PX).toDp() }
    val handOffsetY = with(density) { (handPos.y - HAND_HALF_PX).toDp() }

    Box(
        modifier = Modifier
            .offset(x = handOffsetX, y = handOffsetY)
            .alpha(alpha.value),
    ) {
        Text(text = HAND_EMOJI, fontSize = HAND_FONT_SP.sp)
    }
}

private const val HAND_EMOJI = "👆"
private const val HAND_FONT_SP = 36
private const val HAND_HALF_PX = 24f
private const val TRAVEL_MS = 1200
private const val HOLD_AT_END_MS = 400L
private const val FADE_MS = 250
private const val LOOP_GAP_MS = 250L
private const val TRAIL_STROKE_DP = 3
private const val TRAIL_ALPHA = 0.55f
