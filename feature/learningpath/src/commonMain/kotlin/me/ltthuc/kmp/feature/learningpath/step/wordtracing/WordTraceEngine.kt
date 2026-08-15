package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import me.ltthuc.kmp.feature.learningpath.step.tracing.GuideLayout
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuide
import me.ltthuc.kmp.feature.learningpath.step.tracing.scaledGuidePaths
import kotlin.math.sqrt

/**
 * Stroke-following engine for Duolingo-style word tracing. Turns a [LetterGuide] (authored in a
 * 0..100 viewBox) into per-stroke [StrokeTrace]s already scaled to the live canvas, then maps a
 * finger position to forward progress along the current stroke.
 *
 * No numeric scoring: a stroke is "traced" purely by the finger travelling from its start toward
 * its end while staying inside a tolerance corridor. Progress only ever moves forward (monotonic),
 * so the child can trace in segments — lift the finger and continue — and the fill accumulates.
 *
 * Coordinate space is identical to what [scaledGuidePaths]/`drawLetterGuide` render, so the drawn
 * track and the hit-testing corridor line up exactly.
 */
internal data class TraceSample(val dist: Float, val pos: Offset)

internal class StrokeTrace(
    /** Scaled path in canvas pixels (same as the rendered track). */
    val path: Path,
    val length: Float,
    /** Points sampled along the path by arc-length (ascending), for corridor hit-testing. */
    val samples: List<TraceSample>,
) {
    private val measure = PathMeasure().apply { setPath(path, false) }

    fun position(dist: Float): Offset = measure.getPosition(dist.coerceIn(0f, length))

    fun tangent(dist: Float): Offset = measure.getTangent(dist.coerceIn(0f, length))

    /** Sub-path from the start up to [dist] — the filled (already-traced) portion. */
    fun segmentTo(dist: Float): Path =
        Path().also { measure.getSegment(0f, dist.coerceIn(0f, length), it, true) }

    /** Sub-path from [dist] to the end — the not-yet-traced portion shown as a dashed guide. */
    fun segmentFrom(dist: Float): Path =
        Path().also { measure.getSegment(dist.coerceIn(0f, length), length, it, true) }

    /** Sub-path between two arc-distances (used to gap the dashed guide before the end arrowhead). */
    fun segment(from: Float, to: Float): Path =
        Path().also { measure.getSegment(from.coerceIn(0f, length), to.coerceIn(0f, length), it, true) }

    /** Nearest distance (px) from [p] to this stroke's sampled centerline. */
    fun minDistanceTo(p: Offset): Float {
        var best = Float.MAX_VALUE
        for (s in samples) {
            val d2 = (s.pos - p).getDistanceSquared()
            if (d2 < best) best = d2
        }
        return sqrt(best)
    }

    /**
     * Given a [finger] position and the current accumulated [fromDist], return the farthest forward
     * arc-distance the finger validly reaches: any sample ahead of [fromDist] (within [lookaheadPx],
     * so the child can't skip a big gap) that lies within [corridorPx] of the finger. Returns
     * [fromDist] unchanged when the finger is off the track — progress never rewinds.
     */
    fun advance(finger: Offset, fromDist: Float, corridorPx: Float, lookaheadPx: Float): Float {
        val corridorSq = corridorPx * corridorPx
        val limit = fromDist + lookaheadPx
        var best = fromDist
        for (s in samples) {
            if (s.dist < fromDist) continue
            if (s.dist > limit) break
            if ((s.pos - finger).getDistanceSquared() <= corridorSq && s.dist > best) {
                best = s.dist
            }
        }
        return best
    }
}

/**
 * Builds one [StrokeTrace] per stroke of [guide], scaled to [canvasSize]. Skips degenerate
 * (zero-length) strokes so the caller can index strokes 1:1 with tracable segments.
 */
internal fun buildLetterTrace(
    guide: LetterGuide,
    canvasSize: Size,
    samplesPerStroke: Int = SAMPLES_PER_STROKE,
): List<StrokeTrace> {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return emptyList()
    return scaledGuidePaths(guide, canvasSize).mapNotNull { path ->
        val measure = PathMeasure().apply { setPath(path, false) }
        val len = measure.length
        if (len <= 0f) return@mapNotNull null
        val samples = (0..samplesPerStroke).mapNotNull { i ->
            val d = len * i / samplesPerStroke
            val p = measure.getPosition(d)
            if (p.isSpecified) TraceSample(d, p) else null
        }
        if (samples.isEmpty()) null else StrokeTrace(path, len, samples)
    }
}

/** Y positions (canvas px) of the three handwriting guide lines for the current [canvasSize]. */
internal data class HandwritingLines(val topY: Float, val midY: Float, val baseY: Float)

/**
 * Derives the top/mid/base guide-line Y's from the SAME [GuideLayout] that renders the glyph, so
 * the ruled lines always sit under the letter regardless of canvas aspect ratio. Values come from
 * the lowercase viewBox convention: ascender ~10, x-height (midline) ~42, baseline ~88.
 */
internal fun handwritingLines(canvasSize: Size): HandwritingLines {
    val layout = GuideLayout.forCanvas(canvasSize)
    return HandwritingLines(
        topY = layout.transformPoint(Offset(0f, ASCENDER_Y)).y,
        midY = layout.transformPoint(Offset(0f, MIDLINE_Y)).y,
        baseY = layout.transformPoint(Offset(0f, BASELINE_Y)).y,
    )
}

private const val SAMPLES_PER_STROKE = 96
private const val ASCENDER_Y = 10f
private const val MIDLINE_Y = 42f
private const val BASELINE_Y = 88f
