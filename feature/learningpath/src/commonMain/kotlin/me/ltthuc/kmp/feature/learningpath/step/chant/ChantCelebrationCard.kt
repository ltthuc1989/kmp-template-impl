package me.ltthuc.kmp.feature.learningpath.step.chant

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import me.ltthuc.kmp.core.model.WordTiming
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
    audioPositionMs: Long = 0L,
    wordTimings: List<WordTiming> = emptyList(),
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

    // Karaoke-driven phase: when metadata exists, note position is computed directly from
    // audio playback position + word_timings (no separate Animatable for mid-chant). The
    // note jumps to word[i] at the exact ms the audio reads that word, snapping back to
    // leftEdge between repetitions. When metadata is absent, fall back to a forever-loop.
    val derivedPhase = if (isPlaying && wordTimings.isNotEmpty()) {
        deriveChantPhase(audioPositionMs, wordTimings, cardCount, segmentCount)
    } else {
        0f
    }

    // Last-word arc Animatable: fires once when audio reaches the last word entry.
    // Drives the smooth arc word[last] → rightEdge at 60fps (audio positionMs updates
    // are too sparse to interpolate a short arc smoothly).
    val atLastWord = isPlaying &&
        wordTimings.isNotEmpty() &&
        audioPositionMs >= (wordTimings.lastOrNull()?.startMs ?: Int.MAX_VALUE)
    val lastArcAnim = remember(lesson.id) { Animatable(0f) }
    LaunchedEffect(atLastWord, lesson.id) {
        if (atLastWord) {
            lastArcAnim.snapTo(cardCount.toFloat())
            lastArcAnim.animateTo(
                targetValue = segmentCount.toFloat(),
                animationSpec = tween(LAST_ARC_MS, easing = FastOutSlowInEasing),
            )
        } else {
            lastArcAnim.snapTo(0f)
        }
    }

    // Tail animation: after audio ends mid-bounce, finish one more jump from the last
    // reached word to rightEdge so the bounce visually completes (avoids snapping back
    // to leftEdge the moment audio stops).
    val tailAnim = remember(lesson.id) { Animatable(0f) }
    var lastPlayingPhase by remember(lesson.id) { mutableStateOf(0f) }
    LaunchedEffect(derivedPhase, lastArcAnim.value, isPlaying) {
        if (isPlaying) {
            lastPlayingPhase = if (atLastWord) lastArcAnim.value else derivedPhase
        }
    }
    LaunchedEffect(isPlaying, lesson.id, wordTimings) {
        if (isPlaying) {
            tailAnim.snapTo(0f)
            return@LaunchedEffect
        }
        if (wordTimings.isEmpty() || lastPlayingPhase <= 0f) {
            // Never played — sit at leftEdge.
            tailAnim.snapTo(0f)
            return@LaunchedEffect
        }
        if (lastPlayingPhase >= segmentCount.toFloat()) {
            // Already reached rightEdge during audio — hold there, don't snap to 0.
            tailAnim.snapTo(segmentCount.toFloat())
            return@LaunchedEffect
        }
        // Audio ended mid-bounce — finish to rightEdge.
        tailAnim.snapTo(lastPlayingPhase)
        tailAnim.animateTo(
            targetValue = segmentCount.toFloat(),
            animationSpec = tween(POST_END_BOUNCE_MS, easing = FastOutSlowInEasing),
        )
    }

    val fallbackPhase = rememberFallbackPhase(
        lessonId = lesson.id,
        active = isPlaying && wordTimings.isEmpty(),
        segmentCount = segmentCount,
    )
    val phase: Float = when {
        isPlaying && wordTimings.isNotEmpty() && atLastWord -> lastArcAnim.value
        isPlaying && wordTimings.isNotEmpty() -> derivedPhase
        isPlaying -> fallbackPhase
        wordTimings.isNotEmpty() -> tailAnim.value
        else -> 0f
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
                    // Use (cardCount + 1) so all 5 bounce segments (leftEdge → word1 → ...
                    // → word4 → rightEdge) have equal width = W / (N+1). Cards cluster
                    // closer to center than a SpaceEvenly row.
                    val span = widthPx / (cardCount + 1)
                    List(cardCount) { idx -> span * (idx + 1) }
                }
                val arcHeightPx = with(LocalDensity.current) { ARC_HEIGHT_DP.dp.toPx() }
                val baselineY = with(LocalDensity.current) {
                    (CELEBRATION_HEIGHT_DP - BASELINE_BOTTOM_INSET_DP).dp.toPx()
                }
                val notePathGapPx = with(LocalDensity.current) { NOTE_PATH_GAP_DP.dp.toPx() }

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
                val totalSegments = bouncePoints.size - 1
                val noteX: Float
                val noteY: Float
                if (totalSegments <= 0) {
                    noteX = bouncePoints.first()
                    noteY = baselineY - notePathGapPx
                } else {
                    // Drive note position purely from phase so it stays at rightEdge
                    // after the tail animation finishes (instead of snapping to leftEdge).
                    val segIdx = phase.toInt().coerceIn(0, totalSegments - 1)
                    val segT = (phase - segIdx).coerceIn(0f, 1f)
                    val from = bouncePoints[segIdx]
                    val to = bouncePoints[segIdx + 1]
                    noteX = from + (to - from) * segT
                    noteY = baselineY - notePathGapPx - sin(segT * PI.toFloat()) * arcHeightPx
                }
                MusicNote(
                    modifier = Modifier
                        .size(28.dp)
                        .offset(noteX, noteY),
                )

                // Word cards positioned at exact bounce points so the note lands on each
                // card center (matches the now-even W/5 segment spacing).
                Box(modifier = Modifier.fillMaxSize()) {
                    orderedWords.forEachIndexed { idx, word ->
                        val cardCenterX = cardCenters[idx]
                        val isHit = isPlaying && phase.toInt() == idx + 1
                        WordCelebrationCard(
                            word = word,
                            isHighlighted = isHit,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) {
                                        placeable.place(
                                            x = (cardCenterX - placeable.width / 2f).toInt(),
                                            y = 0,
                                        )
                                    }
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordCelebrationCard(
    word: LessonWord,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            scale.animateTo(1.18f, tween(180, easing = FastOutSlowInEasing))
            scale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = modifier
            .scale(scale.value)
            .width(72.dp)
            .height(CELEBRATION_HEIGHT_DP.dp),
    ) {
        WordDisplayView(
            word = word,
            fontSize = 40.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-16).dp),
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

/**
 * Map audio [positionMs] + per-word [timings] to a continuous note phase in 0..segmentCount.
 *
 * - `timings` is a flat list of repetitions: 4 words × N repeats = 4N entries.
 * - Bounce points: 0 = leftEdge, 1..cardCount = word centers, segmentCount = rightEdge.
 * - When the next entry belongs to a new rep (`nextIdx % cardCount == 0`), the segment
 *   sweeps current word → rightEdge (first half) → snap to leftEdge → word[0] (second half).
 */
private fun deriveChantPhase(
    positionMs: Long,
    timings: List<WordTiming>,
    cardCount: Int,
    segmentCount: Int,
): Float {
    if (timings.isEmpty()) return 0f
    val currentIdx = timings.indexOfLast { it.startMs <= positionMs }
    if (currentIdx < 0) return 0f

    val curWordIdx = currentIdx % cardCount
    val curPhase = (curWordIdx + 1).toFloat()

    val nextIdx = currentIdx + 1
    if (nextIdx >= timings.size) {
        // Last entry: just sit on the word; the caller drives the arc-to-rightEdge
        // via a separate Animatable (audio positionMs updates are too sparse for a
        // smooth 200ms arc, so we use Animatable @ 60fps instead).
        return curPhase
    }

    val isNewRep = (nextIdx % cardCount) == 0
    val segLen = (timings[nextIdx].startMs - timings[currentIdx].startMs).coerceAtLeast(1)
    val t = ((positionMs - timings[currentIdx].startMs).toFloat() / segLen).coerceIn(0f, 1f)

    return if (isNewRep) {
        if (t < 0.5f) {
            curPhase + (segmentCount - curPhase) * (t * 2f)
        } else {
            (t - 0.5f) * 2f
        }
    } else {
        val nextPhase = ((nextIdx % cardCount) + 1).toFloat()
        curPhase + (nextPhase - curPhase) * t
    }
}

@Composable
private fun rememberFallbackPhase(
    lessonId: String,
    active: Boolean,
    segmentCount: Int,
): Float {
    val notePhase = remember(lessonId) { Animatable(0f) }
    LaunchedEffect(active, lessonId) {
        if (active) {
            while (true) {
                notePhase.snapTo(0f)
                notePhase.animateTo(
                    targetValue = segmentCount.toFloat(),
                    animationSpec = tween(
                        durationMillis = segmentCount * SEGMENT_DURATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
        } else {
            notePhase.snapTo(0f)
        }
    }
    return notePhase.value
}

private const val SEGMENT_DURATION_MS = 800
private const val ARC_HEIGHT_DP = 90
private const val NOTE_PATH_GAP_DP = 3
private const val BASELINE_BOTTOM_INSET_DP = 16
private const val CELEBRATION_HEIGHT_DP = 180
private const val LAST_ARC_MS = 400
private const val POST_END_BOUNCE_MS = 200
