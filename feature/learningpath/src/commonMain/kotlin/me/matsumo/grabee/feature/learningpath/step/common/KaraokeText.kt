package me.matsumo.grabee.feature.learningpath.step.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Karaoke-style progressive fill text — tokens stay at a single font size and color-fill from
 * left to right as narration plays. Matches kids reading apps (Duolingo ABC, Khan Academy Kids,
 * Lingokids) karaoke subtitle convention.
 *
 * Behavior:
 *  - `isPlaying = false`: all tokens render at [baseColor], static (no animation).
 *  - `isPlaying = true`: `Animatable<Float>` phase 0 → tokenCount linear over
 *    (tokenCount × [tokenDurationMs]). Each token interpolates baseColor → activeColor as
 *    phase crosses it. When phase reaches end, tokens stay filled at [activeColor]. Toggling
 *    `isPlaying` back to false snaps phase to 0 (reset to static baseColor).
 *
 * No size bump, no oscillation — Story narration and any future "read along" context uses this.
 * Chant screen has its own private `ChantText` with rhythmic wave + bump effect, separate use case.
 *
 * Token splitting: by space, with dash ("-") acting as a token boundary keeping the dash suffix
 * (e.g., "A-a-apple" → ["A-", "a-", "apple"]).
 */
@Suppress("LongParameterList")
@Composable
internal fun KaraokeText(
    text: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    lineHeight: TextUnit = 36.sp,
    tokenDurationMs: Int = 450,
    fontWeight: FontWeight = FontWeight.SemiBold,
    baseColor: Color = MaterialTheme.colorScheme.onSurface,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val tokens = remember(text) { text.tokenize() }
    if (tokens.isEmpty()) return

    val phase = remember(text) { Animatable(0f) }
    LaunchedEffect(text, isPlaying) {
        if (isPlaying) {
            phase.snapTo(0f)
            phase.animateTo(
                targetValue = tokens.size.toFloat(),
                animationSpec = tween(
                    durationMillis = tokens.size * tokenDurationMs,
                    easing = LinearEasing,
                ),
            )
        } else {
            phase.snapTo(0f)
        }
    }

    val annotated = buildAnnotatedString {
        tokens.forEachIndexed { i, token ->
            val fill = (phase.value - i.toFloat()).coerceIn(0f, 1f)
            val color = lerp(baseColor, activeColor, fill)
            withStyle(SpanStyle(color = color, fontSize = fontSize)) {
                append(token)
            }
            if (i < tokens.lastIndex) append(" ")
        }
    }

    Text(
        text = annotated,
        modifier = modifier,
        fontWeight = fontWeight,
        textAlign = TextAlign.Center,
        lineHeight = lineHeight,
    )
}

private fun String.tokenize(): List<String> {
    return split(" ").flatMap { word ->
        if ("-" in word) {
            val parts = word.split("-")
            parts.mapIndexed { i, p -> if (i < parts.lastIndex) "$p-" else p }
                .filter { it.isNotEmpty() }
        } else {
            listOf(word)
        }
    }
}
