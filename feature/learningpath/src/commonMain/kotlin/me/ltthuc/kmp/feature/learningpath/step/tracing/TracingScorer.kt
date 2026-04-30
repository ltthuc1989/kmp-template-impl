package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.vector.PathParser

/**
 * Path-sample distance scoring for letter tracing. Samples N equidistant points along each
 * stroke of the target letter guide, then counts how many of those "ideal" points are within
 * [HIT_RADIUS_DP_SCALED] pixels of ANY user-drawn point. Score = covered / total. Forgiving
 * threshold ([PASS_THRESHOLD] = 0.6) tuned for 3-8yo motor skills.
 *
 * Pros: cross-platform (no bitmap ops), O(n*m) where n ≈ 100 and m ≈ 500 — fast enough per
 * finger-up event. Cons: slightly generous on scribbles that cross many guide points; sufficient
 * for MVP.
 */
internal object TracingScorer {

    const val PASS_THRESHOLD = 0.6f
    private const val SAMPLE_POINTS_PER_STROKE = 40
    private const val HIT_RADIUS_FRACTION = 0.06f // 6% of canvas width

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

        val scaleX = canvasSize.width / 100f
        val scaleY = canvasSize.height / 100f
        val hitRadiusPx = canvasSize.width * HIT_RADIUS_FRACTION
        val hitRadiusSq = hitRadiusPx * hitRadiusPx

        val idealPoints = mutableListOf<Offset>()
        guide.strokes.forEach { stroke ->
            val path = PathParser().parsePathString(stroke.svgPath).toPath()
            val scaled = Path().apply {
                addPath(path)
                transform(Matrix().apply { scale(scaleX, scaleY) })
            }
            val measure = PathMeasure().apply { setPath(scaled, false) }
            val len = measure.length
            if (len <= 0f) return@forEach
            for (i in 0..SAMPLE_POINTS_PER_STROKE) {
                val pos = measure.getPosition(len * i.toFloat() / SAMPLE_POINTS_PER_STROKE)
                if (pos.isSpecified) idealPoints.add(pos)
            }
        }
        if (idealPoints.isEmpty()) return 0f

        val covered = idealPoints.count { ideal ->
            flatUser.any { user ->
                val dx = ideal.x - user.x
                val dy = ideal.y - user.y
                dx * dx + dy * dy <= hitRadiusSq
            }
        }
        return covered.toFloat() / idealPoints.size
    }
}
