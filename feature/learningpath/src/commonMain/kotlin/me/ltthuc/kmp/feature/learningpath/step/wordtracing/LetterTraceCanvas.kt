package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuide
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive single-letter trace surface (Duolingo-style). Shared by the Level 2 word-trace screen
 * (one instance per current letter) and the dev Trace Tester. Owns per-letter state: which stroke is
 * active and how far along it the finger has traced. The finger must follow each stroke's track from
 * start to end (small forward window, no numeric score); completing the last stroke fires
 * [onLetterComplete].
 *
 * [resetKey] identifies the "slot": pass the letter position (word/letter index) or a retrace counter
 * so the same glyph shape can restart — keying on the [guide] instance alone would not reset for a
 * repeated letter (e.g. the two g's in "egg").
 */
@Composable
internal fun LetterTraceCanvas(
    guide: LetterGuide,
    resetKey: Any,
    modifier: Modifier = Modifier,
    showIdleHand: Boolean = true,
    showGlyph: Boolean = true,
    onLetterComplete: () -> Unit = {},
) {
    val state = remember(resetKey) { LetterTraceState() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val traceModel = remember(guide, canvasSize) { buildLetterTrace(guide, canvasSize) }

    // f/t/k/x/y reveal their dashed guide + arrow ONE stroke at a time (the rest of the letter shows
    // as gray only until reached); every other letter shows all strokes' dashes at once. Both trace
    // EVERY stroke and complete after the last one.
    val progressive = remember(guide) { guide.char.lowercaseChar() in PROGRESSIVE_REVEAL }
    // For non-progressive letters the single end-arrow sits on the LAST stroke; pull it back from a
    // junction so it never overlaps another stroke's dash. (Progressive letters draw the arrow on
    // the CURRENT stroke each step → nothing to precompute.)
    val density = LocalDensity.current
    val arrowDists = remember(traceModel, canvasSize, progressive) {
        if (canvasSize == Size.Zero || traceModel.isEmpty() || progressive) {
            emptyList()
        } else {
            val tw = trackWidthPx(canvasSize)
            val gap = with(density) { 2.dp.toPx() }
            val last = traceModel.lastIndex
            traceModel.mapIndexed { idx, s ->
                if (idx == last) computeArrowDist(s, traceModel, idx, tw, gap) else s.length
            }
        }
    }

    SideEffect {
        state.strokes = traceModel
        state.canvasSize = canvasSize
    }

    LaunchedEffect(state.done) { if (state.done) onLetterComplete() }

    // Idle demo: after a few seconds without tracing, a 👆 traces the current stroke once.
    var showHand by remember(resetKey) { mutableStateOf(false) }
    LaunchedEffect(resetKey, state.strokeIndex, state.interactions) {
        showHand = false
        if (showIdleHand && !state.done) {
            delay(IDLE_MS)
            showHand = !state.done
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Key on `state` (recreated per resetKey) so the gesture captures the CURRENT state
                // holder — with pointerInput(Unit) it would keep updating the first letter's state.
                .pointerInput(state) {
                    detectDragGestures(
                        onDragStart = { state.onTouchStart(it) },
                        onDrag = { change, _ ->
                            change.consume()
                            state.onFinger(change.position)
                        },
                    )
                },
        ) {
            drawGuideLines(handwritingLines(size))
            // While the finished letter flies up into the header, keep the ruled lines but drop the
            // ink — the flying copy is what the child sees move.
            if (!showGlyph) return@Canvas

            val trackW = trackWidthPx(size)
            val gapPx = 2.dp.toPx()
            val trackStroke = Stroke(trackW, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val strokeIndex = state.strokeIndex
            val progress = state.progressPx
            val lastIndex = traceModel.lastIndex

            // Gray track for every not-yet-completed stroke — the full letter shape is always visible.
            for (i in strokeIndex until traceModel.size) {
                drawPath(traceModel[i].path, color = TraceTrackGray, style = trackStroke)
            }
            // Completed strokes: solid dark.
            for (i in 0 until minOf(strokeIndex, traceModel.size)) {
                drawPath(traceModel[i].path, color = TraceInkDark, style = trackStroke)
            }
            // Current stroke: dark fill up to the pen.
            val current = traceModel.getOrNull(strokeIndex)
            if (current != null && progress > 0f) {
                drawPath(current.segmentTo(progress), color = TraceInkDark, style = trackStroke)
            }
            // Dashed guide + arrow. Progressive letters (f/t/k/x/y): only the CURRENT stroke has a
            // dash + its own arrow (upcoming strokes stay gray until reached). Others: every
            // not-completed stroke is dashed, with a single arrow on the LAST stroke. Dash stops
            // ~2dp before the arrow.
            // Progressive → only the current stroke is dashed; otherwise every not-completed stroke.
            val dashTo = if (progressive) strokeIndex else lastIndex
            for (i in strokeIndex..dashTo) {
                if (i >= traceModel.size) break
                val stroke = traceModel[i]
                val len = stroke.length
                val from = if (i == strokeIndex) progress else 0f
                val wantArrow = (if (progressive) i == strokeIndex else i == lastIndex) &&
                    len > trackW * ARROW_MIN_LEN
                val aDist = if (wantArrow && !progressive) arrowDists.getOrElse(i) { len } else len
                val hasArrow = wantArrow && aDist > from
                val dashEnd = if (hasArrow) {
                    (aDist - trackW * ARROW_TAIL - gapPx).coerceAtLeast(from)
                } else {
                    len
                }
                if (dashEnd > from) {
                    drawPath(
                        stroke.segment(from, dashEnd),
                        color = TracePenBlue,
                        style = Stroke(
                            width = trackW * DASH_WIDTH_FRACTION,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(trackW * 0.18f, trackW * 0.5f),
                                // Anchor the pattern to the stroke origin so the dashes stay static
                                // instead of crawling forward with the pen (from = pen arc position).
                                phase = from,
                            ),
                        ),
                    )
                }
                if (hasArrow) {
                    val ePos = stroke.position(aDist)
                    val eTan = stroke.tangent(aDist)
                    if (ePos.isSpecified && eTan.isSpecified) {
                        drawEndArrowhead(ePos, eTan, TracePenBlue, trackW)
                    }
                }
            }
            // Pen head on the current stroke (on top) — hidden once the letter is complete.
            if (current != null && !state.done) {
                val pen = current.position(progress)
                val tangent = current.tangent(progress)
                if (pen.isSpecified && tangent.isSpecified) {
                    drawPenHead(pen, tangent, radius = trackW * PEN_RADIUS_FRACTION)
                }
            }
        }
        if (showHand && !state.done) {
            IdleTraceHand(stroke = traceModel.getOrNull(state.strokeIndex))
        }
    }
}

/** Per-letter trace state: active stroke + accumulated forward progress on it. */
private class LetterTraceState {
    var strokeIndex by mutableStateOf(0)
    var progressPx by mutableStateOf(0f)
    var done by mutableStateOf(false)
    var interactions by mutableStateOf(0)

    // Refreshed each composition; read by the gesture handler at touch time.
    var strokes: List<StrokeTrace> = emptyList()
    var canvasSize: Size = Size.Zero

    /** Touch-down: reset idle timer (once per gesture) and trace. */
    fun onTouchStart(pos: Offset) {
        interactions += 1
        onFinger(pos)
    }

    fun onFinger(pos: Offset) {
        if (done) return
        val stroke = strokes.getOrNull(strokeIndex) ?: return
        val trackW = trackWidthPx(canvasSize)
        val corridor = trackW * CORRIDOR_FACTOR
        // Small forward window so the finger must travel the path continuously — it can't jump
        // ahead to the stroke's end (which would let a rough scribble skip whole letters). Wide
        // enough that a fast drag between pointer samples still registers.
        val lookahead = trackW * LOOKAHEAD_TRACK_MULT
        val advanced = stroke.advance(pos, progressPx, corridor, lookahead)
        if (advanced > progressPx) progressPx = advanced
        if (progressPx >= stroke.length * COMPLETE_FRACTION) {
            progressPx = stroke.length
            if (strokeIndex + 1 < strokes.size) {
                strokeIndex += 1
                progressPx = 0f
            } else {
                done = true
            }
        }
    }
}

internal fun DrawScope.drawGuideLines(lines: HandwritingLines) {
    val w = size.width
    drawLine(TraceGuideLineColor, Offset(0f, lines.topY), Offset(w, lines.topY), strokeWidth = 3f)
    drawLine(
        TraceGuideLineColor,
        Offset(0f, lines.midY),
        Offset(w, lines.midY),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)),
    )
    drawLine(TraceGuideLineColor, Offset(0f, lines.baseY), Offset(w, lines.baseY), strokeWidth = 3f)
}

/**
 * Small solid triangle sitting INSIDE the stroke end, pointing along its final tangent. Kept
 * within the track width (half-width < trackW/2, length within the rounded end cap) so it doesn't
 * stick out past the guide shape.
 */
private fun DrawScope.drawEndArrowhead(endPos: Offset, tangent: Offset, color: Color, trackW: Float) {
    val angle = atan2(tangent.y, tangent.x)
    val dirX = cos(angle)
    val dirY = sin(angle)
    // Apex reaches into the round end cap (cap radius ≈ 0.5*trackW) so the arrow touches the
    // stroke's bottom/top edge; the tail sits just behind the centerline endpoint.
    val apex = Offset(endPos.x + dirX * trackW * 0.45f, endPos.y + dirY * trackW * 0.45f)
    val backX = endPos.x - dirX * trackW * ARROW_TAIL
    val backY = endPos.y - dirY * trackW * ARROW_TAIL
    val perpX = -dirY
    val perpY = dirX
    val half = trackW * 0.30f
    val path = Path().apply {
        moveTo(apex.x, apex.y)
        lineTo(backX + perpX * half, backY + perpY * half)
        lineTo(backX - perpX * half, backY - perpY * half)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawPenHead(center: Offset, tangent: Offset, radius: Float) {
    drawCircle(TracePenBlue, radius, center)
    val angle = atan2(tangent.y, tangent.x)
    val tip = Offset(center.x + cos(angle) * radius * 0.55f, center.y + sin(angle) * radius * 0.55f)
    val tail = Offset(center.x - cos(angle) * radius * 0.35f, center.y - sin(angle) * radius * 0.35f)
    val arm = radius * 0.5f
    val spread = 2.45f // ~140°, wings fan backward from the tip
    val wing1 = Offset(tip.x + cos(angle + spread) * arm, tip.y + sin(angle + spread) * arm)
    val wing2 = Offset(tip.x + cos(angle - spread) * arm, tip.y + sin(angle - spread) * arm)
    val armWidth = radius * 0.24f
    drawLine(Color.White, tail, tip, strokeWidth = armWidth, cap = StrokeCap.Round)
    drawLine(Color.White, tip, wing1, strokeWidth = armWidth, cap = StrokeCap.Round)
    drawLine(Color.White, tip, wing2, strokeWidth = armWidth, cap = StrokeCap.Round)
}

@Composable
private fun BoxScope.IdleTraceHand(stroke: StrokeTrace?) {
    if (stroke == null) return
    val progress = remember(stroke) { Animatable(0f) }
    var finished by remember(stroke) { mutableStateOf(false) }
    LaunchedEffect(stroke) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = HAND_TRACE_MS, easing = LinearEasing))
        finished = true
    }
    if (finished) return
    val point = stroke.position(progress.value * stroke.length)
    if (!point.isSpecified) return
    Text(
        text = "👆",
        fontSize = HAND_FONT_SP.sp,
        modifier = Modifier.offset {
            IntOffset(
                x = (point.x - HAND_FONT_SP * 0.5f).roundToInt(),
                y = point.y.roundToInt(),
            )
        },
    )
}

internal fun trackWidthPx(size: Size): Float =
    if (size == Size.Zero) 1f else minOf(size.width, size.height) * TRACK_FRACTION

/**
 * Arc-distance along [stroke] at which to place its end arrow so the arrow (apex reaching ~0.45·
 * trackW ahead) stays at least ~2dp clear of every OTHER stroke's centerline. Returns the stroke
 * end when it's already clear; pulls back along the stroke when the end sits at a junction.
 */
private fun computeArrowDist(
    stroke: StrokeTrace,
    all: List<StrokeTrace>,
    index: Int,
    trackW: Float,
    gapPx: Float,
): Float {
    val len = stroke.length
    val others = all.filterIndexed { j, _ -> j != index }
    if (others.isEmpty()) return len
    val need = gapPx + trackW * 0.15f
    val step = maxOf(trackW * 0.07f, 1.5f)
    val minD = len * 0.4f
    var d = len
    while (d > minD) {
        val p = stroke.position(d)
        val t = stroke.tangent(d)
        if (p.isSpecified && t.isSpecified) {
            val ang = atan2(t.y, t.x)
            val tip = Offset(p.x + cos(ang) * trackW * 0.45f, p.y + sin(ang) * trackW * 0.45f)
            val nearest = others.minOf { minOf(it.minDistanceTo(tip), it.minDistanceTo(p)) }
            if (nearest >= need) return d
        }
        d -= step
    }
    return d
}

// Shared trace palette (used by the canvas + the word header).
internal val TraceInkDark = Color(0xFF4A4A4A)
internal val TracePenBlue = Color(0xFF35B4F0)
internal val TraceTrackGray = Color(0xFFE6E6E6)
internal val TraceGuideLineColor = Color(0xFFDBDBDB)
internal val TraceHighlightGreen = Color(0xFFD8F0A8)
internal val TraceUpcomingGlyphGray = Color(0xFFDDDDDD)

/** Celebration green for a just-finished letter (flying glyph + the brief header flash before it settles to dark). */
internal val TraceDoneGreen = Color(0xFF7DC242)

private const val TRACK_FRACTION = 0.11f
private const val PEN_RADIUS_FRACTION = 0.85f
private const val DASH_WIDTH_FRACTION = 0.18f

// End-arrow (× trackW): shown only when the stroke is longer than ARROW_MIN_LEN. The arrow hugs the
// stroke END — apex reaches into the round cap; its tail is ARROW_TAIL behind the centerline
// endpoint. The dash stops ~2dp before that tail (see gapPx).
private const val ARROW_TAIL = 0.12f
private const val ARROW_MIN_LEN = 1.3f

// Letters that reveal their dashed guide + arrow ONE stroke at a time (rest of the letter stays
// gray until reached) — per the Duolingo references (f/t crossbar, k arm-leg, x/y second diagonal).
// Every stroke is still traced; the letter completes after the last one.
private const val PROGRESSIVE_REVEAL = "ftkxy"
private const val CORRIDOR_FACTOR = 1.0f
private const val LOOKAHEAD_TRACK_MULT = 2.2f
private const val COMPLETE_FRACTION = 0.9f
private const val IDLE_MS = 5_000L
private const val HAND_TRACE_MS = 1_600
private const val HAND_FONT_SP = 40
