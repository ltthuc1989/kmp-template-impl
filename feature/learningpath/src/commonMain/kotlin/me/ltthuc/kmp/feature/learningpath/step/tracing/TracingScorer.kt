package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Bidirectional path-sample scoring for letter tracing. For each stroke in the guide, we sample
 * N equidistant ideal points along the path (transformed via the SAME [GuideLayout.forCanvas]
 * the renderer uses, so coordinates match the ghost letter the child sees).
 *
 * Final score = min(guideCoverage, userCoverage) × strokeCountFactor where:
 *   - guideCoverage = % of ideal points within [HIT_RADIUS_FRACTION] of any user point
 *     (rewards covering the whole letter)
 *   - userCoverage  = % of user points within [HIT_RADIUS_FRACTION] of any ideal point
 *     (penalises scribbles outside the letter envelope)
 *   - strokeCountFactor penalises drawings with wrong number of strokes (e.g. 1-stroke
 *     scribble across a 3-stroke letter B): 1.0 if matched, decays toward 0.7 as mismatch grows
 *
 * Threshold ([PASS_THRESHOLD] = 0.75) tuned to require both good coverage AND correct stroke
 * count for 3-8yo. Min-of-two prevents "trace tiny piece" / "scribble wildly" gaming.
 *
 * Pros: cross-platform, O(n*m) per call (~20k ops, <1ms). Cons: still doesn't validate stroke
 * order or direction (deferred to P3).
 */
internal object TracingScorer {

    const val PASS_THRESHOLD = 0.75f
    private const val SAMPLE_POINTS_PER_STROKE = 40
    private const val HIT_RADIUS_FRACTION = 0.06f // 6% of canvas width
    private const val STROKE_COUNT_PENALTY_PER_DIFF = 0.15f // -15% per stroke off
    private const val STROKE_COUNT_MIN_FACTOR = 0.4f // floor to avoid zero-out

    /**
     * @return a score in 0..1 reflecting how well [userStrokes] cover the ideal [guide] path.
     */
    fun score(
        userStrokes: List<List<Offset>>,
        guide: LetterGuide,
        canvasSize: Size,
    ): Float {
        if (guide.strokes.isEmpty()) return 0f
        val flatUser = userStrokes.flatten()
        if (flatUser.isEmpty()) return 0f

        val layout = GuideLayout.forCanvas(canvasSize)
        val hitRadiusPx = canvasSize.width * HIT_RADIUS_FRACTION
        val hitRadiusSq = hitRadiusPx * hitRadiusPx

        val idealPoints = mutableListOf<Offset>()
        guide.strokes.forEach { stroke ->
            val path = PathParser().parsePathString(stroke.svgPath).toPath()
            val scaled = layout.transformPath(path)
            val measure = PathMeasure().apply { setPath(scaled, false) }
            val len = measure.length
            if (len <= 0f) return@forEach
            for (i in 0..SAMPLE_POINTS_PER_STROKE) {
                val pos = measure.getPosition(len * i.toFloat() / SAMPLE_POINTS_PER_STROKE)
                if (pos.isSpecified) idealPoints.add(pos)
            }
        }
        if (idealPoints.isEmpty()) return 0f

        val guideCoverage = idealPoints.count { ideal ->
            flatUser.any { user ->
                val dx = ideal.x - user.x
                val dy = ideal.y - user.y
                dx * dx + dy * dy <= hitRadiusSq
            }
        }.toFloat() / idealPoints.size

        val userCoverage = flatUser.count { user ->
            idealPoints.any { ideal ->
                val dx = ideal.x - user.x
                val dy = ideal.y - user.y
                dx * dx + dy * dy <= hitRadiusSq
            }
        }.toFloat() / flatUser.size

        val coverageScore = minOf(guideCoverage, userCoverage)
        val strokeDiff = kotlin.math.abs(userStrokes.size - guide.strokes.size)
        val strokeCountFactor = (1f - strokeDiff * STROKE_COUNT_PENALTY_PER_DIFF)
            .coerceAtLeast(STROKE_COUNT_MIN_FACTOR)
        return coverageScore * strokeCountFactor
    }
}
