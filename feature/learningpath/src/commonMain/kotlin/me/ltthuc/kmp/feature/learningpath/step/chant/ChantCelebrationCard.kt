package me.ltthuc.kmp.feature.learningpath.step.chant

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
import kotlin.math.PI
import kotlin.math.sin

/**
 * Final celebration slide for ChantScreen.
 *
 * Renders 4 word cards in a horizontal row in `lesson.chantOrder`. A music note ♪ travels
 * left→right above the row tracing a series of bouncing arcs (one per card), with a dotted
 * green trail connecting them — like the Oxford Phonics chant rhythm.
 *
 * When [isPlaying] is true, the note loops; the card the note is currently above pops with
 * a scale animation. When false, note + cards stay at rest at the start.
 */
@Composable
internal fun ChantCelebrationCard(
    lesson: PhonicsLesson,
    isPlaying: Boolean,
) {
    val orderedWords = remember(lesson.id) {
        val order = lesson.chantOrder.takeIf { it.size == lesson.words.size }
            ?: lesson.words.indices.toList()
        order.mapNotNull { idx -> lesson.words.getOrNull(idx) }
    }
    if (orderedWords.isEmpty()) return

    val cardCount = orderedWords.size
    // Path has cardCount + 1 segments (leftEdge → word[0] → … → word[last] → rightEdge).
    val segmentCount = cardCount + 1
    val totalDuration = segmentCount * SEGMENT_DURATION_MS

    // notePhase: 0f..segmentCount.toFloat(); integer part = current segment, fractional = progress within segment.
    val notePhase = remember(lesson.id) { Animatable(0f) }
    LaunchedEffect(isPlaying, lesson.id) {
        if (isPlaying) {
            while (true) {
                notePhase.snapTo(0f)
                notePhase.animateTo(
                    targetValue = segmentCount.toFloat(),
                    animationSpec = tween(durationMillis = totalDuration, easing = FastOutSlowInEasing),
                )
            }
        } else {
            notePhase.snapTo(0f)
        }
    }

    StoryStyleCard(aspectRatio = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CELEBRATION_HEIGHT_DP.dp),
            ) {
                val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                val cardCenters = remember(widthPx, cardCount) {
                    val span = widthPx / cardCount
                    List(cardCount) { idx -> span * (idx + 0.5f) }
                }
                val arcHeightPx = with(LocalDensity.current) { ARC_HEIGHT_DP.dp.toPx() }
                val baselineY = with(LocalDensity.current) { (CELEBRATION_HEIGHT_DP - 8).dp.toPx() }

                // Bounce points: leftEdge → word[0] → … → word[last] → rightEdge.
                val bouncePoints = remember(widthPx, cardCenters) {
                    buildList {
                        add(0f)
                        addAll(cardCenters)
                        add(widthPx)
                    }
                }

                // Dotted arc trail spanning every bounce point.
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path()
                    path.moveTo(bouncePoints[0], baselineY)
                    for (i in 1 until bouncePoints.size) {
                        val prevX = bouncePoints[i - 1]
                        val curX = bouncePoints[i]
                        val midX = (prevX + curX) / 2f
                        val midY = baselineY - arcHeightPx
                        path.quadraticBezierTo(midX, midY, curX, baselineY)
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)),
                        ),
                    )
                }

                // Music note: bounces between consecutive bounce points (cardCount + 1 segments).
                val phase = notePhase.value
                val totalSegments = bouncePoints.size - 1
                val noteX: Float
                val noteY: Float
                if (!isPlaying || totalSegments <= 0) {
                    noteX = bouncePoints.first()
                    noteY = baselineY
                } else {
                    val segIdx = phase.toInt().coerceIn(0, totalSegments - 1)
                    val segT = (phase - segIdx).coerceIn(0f, 1f)
                    val from = bouncePoints[segIdx]
                    val to = bouncePoints[segIdx + 1]
                    noteX = from + (to - from) * segT
                    noteY = baselineY - sin(segT * PI.toFloat()) * arcHeightPx
                }
                MusicNote(
                    modifier = Modifier
                        .size(28.dp)
                        .offset(noteX, noteY),
                )

                // Word cards row (overlay row anchored at bottom).
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    orderedWords.forEachIndexed { idx, word ->
                        // Note arrives at word[idx] at end of segment idx, so it's "hit" during segment idx+1.
                        val isHit = isPlaying && phase.toInt() == idx + 1
                        WordCelebrationCard(
                            word = word,
                            isHighlighted = isHit,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Sing it together!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WordCelebrationCard(word: LessonWord, isHighlighted: Boolean) {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            scale.animateTo(1.18f, tween(180, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = Modifier
            .scale(scale.value)
            .width(72.dp)
            .height(CELEBRATION_HEIGHT_DP.dp),
    ) {
        WordDisplayView(
            word = word,
            fontSize = 40.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        Text(
            text = word.word,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun MusicNote(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "note-wiggle")
    val rotation by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "note-rotation",
    )
    Box(modifier = modifier.graphicsLayer { rotationZ = rotation }) {
        Text(
            text = "♪",
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun Modifier.offset(x: Float, y: Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.place(
                x = (x - placeable.width / 2f).toInt(),
                y = (y - placeable.height / 2f).toInt(),
            )
        }
    }

private const val SEGMENT_DURATION_MS = 800
private const val ARC_HEIGHT_DP = 60
private const val CELEBRATION_HEIGHT_DP = 180
