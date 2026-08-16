package me.ltthuc.kmp.core.ui.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp

/**
 * Dissolves the bottom [height] of the content into transparency — used where something floats over
 * a scrolling list (the pill nav bar) and items should melt away as they slide underneath instead of
 * being chopped off by a hard edge.
 *
 * The content is rendered into an offscreen layer first, then a top-to-transparent gradient is
 * composited with [BlendMode.DstIn] so it eats the layer's alpha. This fades what is DRAWN, so
 * whatever sits behind (the screen background) shows through — a scrim of the background color would
 * only work while the background stays flat.
 */
fun Modifier.fadeOutBottom(height: Dp): Modifier {
    if (height <= Dp.Hairline) return this
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadePx = height.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = size.height - fadePx,
                    endY = size.height,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
}
