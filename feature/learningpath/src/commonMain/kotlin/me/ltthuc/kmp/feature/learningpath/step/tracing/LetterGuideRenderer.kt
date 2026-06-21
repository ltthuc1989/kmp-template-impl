package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Memo for parsed stroke paths. `parsePathString(...).toPath()` is pure CPU work that depends only
 * on the (constant) svgPath string, yet [drawLetterGuide]/[drawGhostLetter] run every draw frame
 * during the trim animation — re-parsing 52 letters' paths 60×/s is wasted main-thread work.
 * Letter paths come from a fixed table (≤52 letters × a few strokes), so the cache stays tiny.
 * The cached [Path] is only ever read (copied via `addPath`), never mutated, and drawing is
 * single-threaded on the UI thread, so a plain map is safe here.
 */
private val parsedStrokePathCache = mutableMapOf<String, Path>()

private fun strokePath(svgPath: String): Path =
    parsedStrokePathCache.getOrPut(svgPath) { PathParser().parsePathString(svgPath).toPath() }

/**
 * Per-stroke paths scaled to [canvasSize], in draw order. Same scaling [drawLetterGuide] uses, so an
 * overlay (e.g. the idle guide hand) can ride exactly along the rendered guide via [PathMeasure].
 */
internal fun scaledGuidePaths(guide: LetterGuide, canvasSize: Size): List<Path> {
    val layout = GuideLayout.forCanvas(canvasSize)
    return guide.strokes.map { layout.transformPath(strokePath(it.svgPath)) }
}

/**
 * Renders a [LetterGuide] into a [DrawScope] with 4 layers per stroke:
 *   1. Soft container halo (gives the guide a tactile "pillow" look).
 *   2. Dashed center line showing the intended trace path.
 *   3. Solid-fill trim progress (driven by [animationProgress]).
 *   4. Arrowhead at the end of each stroke to communicate direction.
 *
 * Badges (1/2/3/4 numbers) are NOT drawn here; they overlay via [StrokeNumberBadges].
 *
 * @param canvasSize pixel size of the enclosing Canvas — used to scale 0..100 viewBox paths.
 * @param animationProgress 0..1 fraction across all strokes combined. 0 = only dashed preview;
 *                          1 = all strokes fully filled.
 */
internal fun DrawScope.drawLetterGuide(
    guide: LetterGuide,
    canvasSize: Size,
    animationProgress: Float,
    primaryColor: Color,
    haloColor: Color,
    strokeWidthPx: Float = 22f,
    dashStrokeWidthPx: Float = 2.5f,
    showArrows: Boolean = true,
) {
    val layout = GuideLayout.forCanvas(canvasSize)
    val strokeCount = guide.strokes.size
    val minLenForArrow = strokeWidthPx * 1.5f

    val scaledPaths = guide.strokes.map { stroke ->
        layout.transformPath(strokePath(stroke.svgPath))
    }

    // Auto-detect per-stroke "inset" needed so the arrow (tip + wings) does NOT collide with
    // other strokes' bodies or arrows. Strokes that don't collide get inset = 0 and render
    // identically to the original (arrow at endpoint, full stroke length).
    val insets = if (showArrows) {
        computeArrowInsets(scaledPaths, strokeWidthPx, minLenForArrow)
    } else {
        List(scaledPaths.size) { 0f }
    }

    // Build per-stroke visible (drawable) path — trimmed to (len - inset) when inset > 0
    // so nothing sticks out past the arrow. If inset == 0, visiblePath == scaledPath.
    data class StrokeRender(
        val visiblePath: Path,
        val arrowTip: Offset?,
        val arrowTangent: Offset?,
    )
    val renderData = scaledPaths.mapIndexed { idx, path ->
        val measure = PathMeasure().apply { setPath(path, false) }
        val len = measure.length
        val inset = insets[idx]
        val showArrow = showArrows && len > minLenForArrow
        val tipDistance = (len - inset).coerceAtLeast(0f)
        val visible = if (inset > 0f && tipDistance > 0f) {
            Path().also { measure.getSegment(0f, tipDistance, it, true) }
        } else {
            path
        }
        val tip = if (showArrow) measure.getPosition(tipDistance) else Offset.Unspecified
        val tangent = if (showArrow) measure.getTangent(tipDistance) else Offset.Unspecified
        StrokeRender(
            visiblePath = visible,
            arrowTip = if (tip.isSpecified) tip else null,
            arrowTangent = if (tangent.isSpecified) tangent else null,
        )
    }

    renderData.forEach { data ->
        drawPath(
            data.visiblePath,
            color = haloColor,
            style = Stroke(
                width = strokeWidthPx + 6f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }

    renderData.forEachIndexed { idx, data ->
        drawPath(
            data.visiblePath,
            color = primaryColor,
            style = Stroke(
                width = dashStrokeWidthPx,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            ),
        )
        val strokeProgress = ((animationProgress * strokeCount) - idx).coerceIn(0f, 1f)
        if (strokeProgress > 0f) {
            val measure = PathMeasure().apply { setPath(data.visiblePath, false) }
            val trimmed = Path()
            measure.getSegment(0f, measure.length * strokeProgress, trimmed, true)
            drawPath(
                trimmed,
                color = primaryColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }

    renderData.forEach { data ->
        val tip = data.arrowTip
        val tangent = data.arrowTangent
        if (tip != null && tangent != null) {
            drawArrowhead(tip, tangent, primaryColor, strokeWidthPx)
        }
    }
}

/**
 * Returns per-stroke inset (in pixels) such that the arrow drawn at `len - inset` does NOT
 * collide with any other stroke's body or arrow. Iteratively grows inset for each stroke
 * until its arrow geometry is clear. Returns 0f for strokes that never collide so the
 * original arrow-at-endpoint rendering is preserved for well-spaced letters.
 */
@Suppress("LongMethod", "NestedBlockDepth", "CyclomaticComplexMethod")
private fun computeArrowInsets(
    paths: List<Path>,
    strokeWidthPx: Float,
    minLenForArrow: Float,
): List<Float> {
    if (paths.size <= 1) return List(paths.size) { 0f }

    val armLength = strokeWidthPx * 0.9f
    val spreadRad = (PI * 0.78f).toFloat()
    val collisionRadius = strokeWidthPx * 0.55f
    val maxInset = strokeWidthPx * 1.2f
    val insetStep = strokeWidthPx * 0.25f
    val samplesPerPath = 24

    val measures = paths.map { PathMeasure().apply { setPath(it, false) } }
    val lens = measures.map { it.length }
    val samples = measures.mapIndexed { i, m ->
        val len = lens[i]
        if (len <= 0f) return@mapIndexed emptyList()
        (0..samplesPerPath).mapNotNull { j ->
            val pos = m.getPosition(len * j.toFloat() / samplesPerPath)
            if (pos.isSpecified) pos else null
        }
    }

    return paths.indices.map { i ->
        val len = lens[i]
        if (len < minLenForArrow) return@map 0f

        var inset = 0f
        while (inset <= maxInset) {
            val tipDist = (len - inset).coerceAtLeast(0f)
            val tip = measures[i].getPosition(tipDist)
            val tangent = measures[i].getTangent(tipDist)
            if (!tip.isSpecified || !tangent.isSpecified) break

            val angle = atan2(tangent.y, tangent.x)
            val wing1 = Offset(
                tip.x + cos(angle + spreadRad) * armLength,
                tip.y + sin(angle + spreadRad) * armLength,
            )
            val wing2 = Offset(
                tip.x + cos(angle - spreadRad) * armLength,
                tip.y + sin(angle - spreadRad) * armLength,
            )
            val checkPoints = listOf(
                tip,
                wing1,
                wing2,
                Offset((tip.x + wing1.x) / 2f, (tip.y + wing1.y) / 2f),
                Offset((tip.x + wing2.x) / 2f, (tip.y + wing2.y) / 2f),
            )

            val rSq = collisionRadius * collisionRadius
            var collides = false
            for (j in paths.indices) {
                if (j == i) continue
                // Skip samples near the neighbor's own arrow tip — both arrows pulling back
                // in a joint is handled symmetrically by each stroke's own inset.
                val neighborSamples = samples[j]
                for (ap in checkPoints) {
                    for (sp in neighborSamples) {
                        val dx = ap.x - sp.x
                        val dy = ap.y - sp.y
                        if (dx * dx + dy * dy < rSq) {
                            collides = true
                            break
                        }
                    }
                    if (collides) break
                }
                if (collides) break
            }

            if (!collides) return@map inset
            inset += insetStep
        }
        maxInset
    }
}

private fun DrawScope.drawArrowhead(
    tip: Offset,
    tangent: Offset,
    color: Color,
    baseWidth: Float,
) {
    val angle = atan2(tangent.y, tangent.x)
    val armLength = baseWidth * 0.9f
    val spreadRad = (PI * 0.78f).toFloat()
    val wing1 = Offset(
        x = tip.x + cos(angle + spreadRad) * armLength,
        y = tip.y + sin(angle + spreadRad) * armLength,
    )
    val wing2 = Offset(
        x = tip.x + cos(angle - spreadRad) * armLength,
        y = tip.y + sin(angle - spreadRad) * armLength,
    )
    val armStrokeWidth = baseWidth * 0.28f
    drawLine(
        color = color,
        start = tip,
        end = wing1,
        strokeWidth = armStrokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = tip,
        end = wing2,
        strokeWidth = armStrokeWidth,
        cap = StrokeCap.Round,
    )
}

/**
 * Overlays numbered circle badges on top of a [drawLetterGuide] canvas so kids can follow the
 * stroke order. Call inside a Box that wraps both the Canvas AND this composable at the same
 * size — badges position themselves via `Modifier.offset` relative to the Box.
 */
@Composable
internal fun BoxScope.StrokeNumberBadges(
    guide: LetterGuide,
    canvasSizePx: Size,
    badgeDiameter: Int = 20,
) {
    val density = LocalDensity.current
    val fontSizeSp = (badgeDiameter * 0.6f).coerceAtLeast(6f).sp
    val layout = GuideLayout.forCanvas(canvasSizePx)
    guide.strokes.forEachIndexed { idx, stroke ->
        val pos = layout.transformPoint(stroke.badgeAt)
        val xPx = pos.x
        val yPx = pos.y
        val halfDiameterPx = with(density) { (badgeDiameter / 2f).dp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (xPx - halfDiameterPx).roundToInt(),
                        y = (yPx - halfDiameterPx).roundToInt(),
                    )
                }
                .size(badgeDiameter.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(stroke.badgeColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = (idx + 1).toString(),
                color = Color.White,
                fontSize = fontSizeSp,
                lineHeight = fontSizeSp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                    ),
                ),
            )
        }
    }
}

/**
 * Returns a soft container/surface color derived from [base] — a watered-down version suitable
 * as the halo layer behind a letter guide. Keeps the tile legible on the primary-container
 * backdrop used by StepChrome's PuffySurface.
 */
@Composable
internal fun letterGuideHaloColor(base: Color = MaterialTheme.colorScheme.primaryContainer): Color =
    base

/**
 * Draws a solid "ghost" letter silhouette using the same Zaner-Bloser stroke paths the guide
 * card renders. Thick stroke (round cap + round join) makes the letter read as one filled shape.
 * Uses the same [GuideLayout] uniform scale as [drawLetterGuide] so guide-card and practice-canvas
 * show identical shapes at the same relative position.
 */
internal fun DrawScope.drawGhostLetter(
    guide: LetterGuide,
    canvasSize: Size,
    color: Color,
    strokeWidthPx: Float,
) {
    val layout = GuideLayout.forCanvas(canvasSize)
    val opaqueColor = color.copy(alpha = 1f)
    val layerAlpha = color.alpha
    val bounds = Rect(0f, 0f, canvasSize.width, canvasSize.height)
    val layerPaint = Paint().apply { alpha = layerAlpha }
    val capRadiusPx = strokeWidthPx / 2f

    // Render strokes into an offscreen layer with FULL opacity, then composite the layer
    // with the source color's alpha. This makes overlapping stroke geometry (D, B, P, R…)
    // union into one solid shape instead of double-blending — eliminates the dark "hot spot"
    // at shared endpoints. After each stroke, drop a full circle of radius = strokeWidth/2
    // at both endpoints: at non-shared endpoints this just overlaps the round cap (no visual
    // change); at shared endpoints it fills the quadrant gap two perpendicular round caps
    // would otherwise leave, so corners read as smooth rounded blobs instead of split lobes.
    drawIntoCanvas { canvas ->
        canvas.saveLayer(bounds, layerPaint)
        guide.strokes.forEach { stroke ->
            val scaled = layout.transformPath(strokePath(stroke.svgPath))
            drawPath(
                scaled,
                color = opaqueColor,
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            val measure = PathMeasure().apply { setPath(scaled, false) }
            val len = measure.length
            if (len > 0f) {
                val startPos = measure.getPosition(0f)
                val endPos = measure.getPosition(len)
                if (startPos.isSpecified) {
                    drawCircle(opaqueColor, capRadiusPx, startPos)
                }
                if (endPos.isSpecified) {
                    drawCircle(opaqueColor, capRadiusPx, endPos)
                }
            }
        }
        canvas.restore()
    }
}

/**
 * Uniform-scale layout for rendering a letter guide authored in 0..100 viewBox onto an arbitrary
 * canvas. Preserves aspect ratio (letter stays square regardless of canvas aspect) and centers
 * the letter in BOTH axes within the canvas — a wide guide card shows a square letter with empty
 * whitespace on the left/right instead of a stretched letter that fills the full width.
 */
internal data class GuideLayout(
    val scale: Float,
    val offsetX: Float,
    val offsetY: Float,
) {
    fun transformPath(raw: Path): Path {
        val scaled = Path().apply {
            addPath(raw)
            transform(Matrix().apply { scale(this@GuideLayout.scale, this@GuideLayout.scale) })
        }
        return Path().apply { addPath(scaled, Offset(offsetX, offsetY)) }
    }

    fun transformPoint(p: Offset): Offset =
        Offset(p.x * scale + offsetX, p.y * scale + offsetY)

    companion object {
        private const val VIEW_BOX_SIZE = 100f

        fun forCanvas(canvasSize: Size): GuideLayout {
            if (canvasSize.width <= 0f || canvasSize.height <= 0f) {
                return GuideLayout(scale = 1f, offsetX = 0f, offsetY = 0f)
            }
            val scale = minOf(canvasSize.width, canvasSize.height) / VIEW_BOX_SIZE
            val offsetX = (canvasSize.width - VIEW_BOX_SIZE * scale) / 2f
            val offsetY = (canvasSize.height - VIEW_BOX_SIZE * scale) / 2f
            return GuideLayout(scale = scale, offsetX = offsetX, offsetY = offsetY)
        }
    }
}
