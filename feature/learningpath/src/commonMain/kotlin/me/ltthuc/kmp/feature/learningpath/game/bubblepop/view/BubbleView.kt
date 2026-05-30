package me.ltthuc.kmp.feature.learningpath.game.bubblepop.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Glassy translucent floating bubble — body lets the background show through, only the
 * rim + highlight spots are clearly visible. Mirrors a real soap bubble: thin bright
 * outline, faint inner tint, two white shine spots, optional bottom-right depth shade.
 *
 * The inner tint is a soft pastel hue ([tint]); pick varying tints per bubble so a
 * cluster reads as a playful rainbow rather than monochrome cyan against the ocean.
 *
 * Sized via [diameter]; the parent (BubbleCanvas) positions the box via offset.
 */
@Composable
internal fun BubbleView(
    letter: String,
    modifier: Modifier = Modifier,
    diameter: Dp = BUBBLE_DIAMETER_DP.dp,
    tint: Color = BUBBLE_DEFAULT_TINT,
) {
    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Faint inner pastel tint — fades to transparent so the ocean shows through.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.30f),
                        tint.copy(alpha = 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(center.x - radius * 0.20f, center.y - radius * 0.20f),
                    radius = radius * 1.10f,
                ),
                radius = radius,
                center = center,
            )

            // Bright rim — main visible structure.
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = radius - 1.5.dp.toPx(),
                center = center,
                style = Stroke(width = 2.5.dp.toPx()),
            )

            // Subtle inner darker ring just inside the rim — gives depth.
            drawCircle(
                color = Color(0xFF0E5562).copy(alpha = 0.28f),
                radius = radius - 4.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Top-left bright shine — primary glass highlight.
            drawCircle(
                color = Color.White.copy(alpha = 0.85f),
                radius = radius * 0.22f,
                center = Offset(center.x - radius * 0.38f, center.y - radius * 0.40f),
            )
            // Smaller secondary sparkle.
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = radius * 0.09f,
                center = Offset(center.x - radius * 0.18f, center.y - radius * 0.55f),
            )

            // Bottom-right shimmer arc — thin crescent giving roundness cue.
            drawArc(
                color = Color.White.copy(alpha = 0.45f),
                startAngle = 35f,
                sweepAngle = 60f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 4.dp.toPx(), center.y - radius + 4.dp.toPx()),
                size = Size(
                    width = (radius - 4.dp.toPx()) * 2,
                    height = (radius - 4.dp.toPx()) * 2,
                ),
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // Single character display — upper OR lower case is decided per-spawn by the spawner.
        // The kid distinguishes letterforms by separate bubbles (e.g. one "A" + one "a"),
        // not a combined "Aa" inside the same bubble.
        Text(
            text = letter,
            fontSize = (diameter.value * 0.45f).sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.55f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f,
                ),
            ),
        )
    }
}

// Bubble 30% smaller vs v5: 84 → 60dp (user feedback round 5b)
internal const val BUBBLE_DIAMETER_DP = 60

/** Soft pastel palette — pick per-bubble so a screenful reads as rainbow soap bubbles. */
internal val BUBBLE_TINT_PALETTE: List<Color> = listOf(
    Color(0xFFFFC8DD), // pink
    Color(0xFFBDE0FE), // sky blue
    Color(0xFFCDB4DB), // lavender
    Color(0xFFFFD6A5), // peach
    Color(0xFFCAFFBF), // mint
    Color(0xFFFDFFB6), // lemon
)

private val BUBBLE_DEFAULT_TINT = Color(0xFFB8F5FF)

/** Deterministic tint for a bubble id so re-renders don't reshuffle the colors. */
internal fun bubbleTintFor(id: Int): Color =
    BUBBLE_TINT_PALETTE[((id % BUBBLE_TINT_PALETTE.size) + BUBBLE_TINT_PALETTE.size) % BUBBLE_TINT_PALETTE.size]
