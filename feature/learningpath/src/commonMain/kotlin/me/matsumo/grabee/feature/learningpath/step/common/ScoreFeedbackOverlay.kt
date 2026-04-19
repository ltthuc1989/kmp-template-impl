package me.matsumo.grabee.feature.learningpath.step.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen score feedback overlay — replaces traditional Dialog.
 * Pattern used by Duolingo ABC, Lingokids, Khan Academy Kids: immersive takeover with
 * confetti particles + bouncy scale-in of puffy result card. Reusable across scoring step screens
 * (Vocabulary, Identify, Blending, Matching...).
 *
 * Render nothing when [feedback] is null. Animated entry = fadeIn + bouncy scaleIn from 0.85f.
 *
 * @param feedback current score result (Success or Fail) — null = hidden
 * @param onDismiss fires when user dismisses by tapping outside card (currently blocked — primary only)
 * @param onPrimary fires when primary button pressed (Keep Going / Retry)
 */
@Composable
internal fun ScoreFeedbackOverlay(
    feedback: ScoreFeedback?,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
) {
    AnimatedVisibility(
        visible = feedback != null,
        enter = fadeIn() + scaleIn(
            initialScale = 0.85f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        ),
        exit = fadeOut() + scaleOut(targetScale = 0.85f),
    ) {
        val current = feedback ?: return@AnimatedVisibility
        ScoreFeedbackContent(
            feedback = current,
            onDismiss = onDismiss,
            onPrimary = onPrimary,
        )
    }
}

@Composable
private fun ScoreFeedbackContent(
    feedback: ScoreFeedback,
    onDismiss: () -> Unit,
    onPrimary: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
    ) {
        // Confetti particles behind card (Success only)
        if (feedback is ScoreFeedback.Success && feedback.decoration == Decoration.Confetti) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())
        }
        PuffySurface(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(fraction = 0.82f)
                .padding(horizontal = 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}, // block tap-through on card
                ),
            shape = RoundedCornerShape(40.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 32.dp,
            shadowTint = MaterialTheme.colorScheme.primary,
            shadowAlpha = 0.40f,
            topHighlightHeight = 24.dp,
            topHighlightAlpha = 0.95f,
            bottomShadeHeight = 24.dp,
            bottomShadeAlpha = 0.18f,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                DecorationRow(feedback = feedback)
                Spacer(Modifier.height(16.dp))
                Text(text = feedback.heroEmoji, fontSize = 80.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = feedback.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = when (feedback) {
                        is ScoreFeedback.Success -> MaterialTheme.colorScheme.primary
                        is ScoreFeedback.Fail -> MaterialTheme.colorScheme.primary
                    },
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = feedback.subtitle,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                PrimaryActionButton(label = feedback.primaryLabel, onClick = onPrimary)
            }
        }
    }
}

@Composable
private fun DecorationRow(feedback: ScoreFeedback) {
    if (feedback !is ScoreFeedback.Success) return
    when (feedback.decoration) {
        Decoration.Stars -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { Text(text = "⭐", fontSize = 24.sp) }
            }
        }
        Decoration.Gift -> Text(text = "🎁", fontSize = 40.sp)
        Decoration.Confetti, Decoration.None -> Spacer(Modifier.height(0.dp))
    }
}

@Composable
private fun PrimaryActionButton(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 14.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.55f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.3f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.30f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * Simple confetti particle system rendered on Canvas.
 * 24 particles, each with random start X + phase offset; loops vertically with slight horizontal drift.
 */
@Composable
private fun ConfettiCanvas(modifier: Modifier = Modifier) {
    val particles = remember {
        List(PARTICLE_COUNT) { index ->
            Particle(
                startXFraction = Random.nextFloat(),
                driftAmplitude = Random.nextFloat() * 0.12f,
                phaseOffset = Random.nextFloat(),
                colorIndex = index % 4,
                sizeDp = 6 + Random.nextInt(8), // 6-14dp
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = CYCLE_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val palette = listOf(primary, secondary, tertiary, primaryContainer)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        particles.forEach { particle ->
            val localPhase = (phase + particle.phaseOffset) % 1f
            val baseX = particle.startXFraction * width
            val driftX = sin(localPhase * Math.PI * 2).toFloat() * particle.driftAmplitude * width
            val x = baseX + driftX
            val y = localPhase * (height + 200f) - 100f
            drawCircle(
                color = palette[particle.colorIndex],
                radius = particle.sizeDp * density,
                center = Offset(x, y),
            )
        }
    }
}

private data class Particle(
    val startXFraction: Float,
    val driftAmplitude: Float,
    val phaseOffset: Float,
    val colorIndex: Int,
    val sizeDp: Int,
)

private const val PARTICLE_COUNT = 24
private const val CYCLE_MS = 3500
private const val density = 1.5f // approximate; particles size is already in "Float px" at LTR density

// ---------- Data API ----------

internal sealed interface ScoreFeedback {
    val title: String
    val subtitle: String
    val heroEmoji: String
    val primaryLabel: String

    data class Success(
        override val title: String,
        override val subtitle: String,
        override val heroEmoji: String = "🎉",
        override val primaryLabel: String,
        val decoration: Decoration = Decoration.Confetti,
    ) : ScoreFeedback

    data class Fail(
        override val title: String,
        override val subtitle: String,
        override val heroEmoji: String = "😔",
        override val primaryLabel: String,
    ) : ScoreFeedback
}

internal enum class Decoration { None, Confetti, Stars, Gift }
