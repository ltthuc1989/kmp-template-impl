package me.ltthuc.kmp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Replicates a CSS multi-layer puffy box-shadow for "3D" tappable surfaces:
 * - Outer drop shadow via [Modifier.shadow]
 * - Inner top highlight: gradient strip (white → transparent)
 * - Inner bottom shade: gradient strip (transparent → black)
 *
 * Shared core:ui copy of the learning-path step button style so non-learningpath
 * modules can reuse it (feature → feature deps are banned).
 */
@Composable
fun PuffySurface(
    shape: Shape,
    containerColor: Color,
    shadowElevation: Dp,
    shadowTint: Color,
    shadowAlpha: Float,
    topHighlightHeight: Dp,
    topHighlightAlpha: Float,
    bottomShadeHeight: Dp,
    bottomShadeAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = shadowTint.copy(alpha = shadowAlpha * 0.6f),
                spotColor = shadowTint.copy(alpha = shadowAlpha),
            )
            .clip(shape)
            .background(containerColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(topHighlightHeight)
                .align(Alignment.TopStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = topHighlightAlpha),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomShadeHeight)
                .align(Alignment.BottomStart)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = bottomShadeAlpha),
                        ),
                    ),
                ),
        )
        content()
    }
}
