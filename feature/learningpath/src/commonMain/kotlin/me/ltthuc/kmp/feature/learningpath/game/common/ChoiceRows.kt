package me.ltthuc.kmp.feature.learningpath.game.common

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Lays the answer choices out on ONE row when they fit, otherwise [perRowWhenWrapped] per row.
 * Every row is centred.
 *
 * User báo 2026-10-03: từ cấp 5 dài (`competition`, `television`) — hai thẻ Pick Word chen một
 * hàng thì chữ trong thẻ bị bẻ xuống dòng. Đáp án chỉ được xuống HÀNG, chữ trong thẻ thì không.
 * Children are measured at their natural width, so each choice's text must be single-line
 * (`maxLines = 1, softWrap = false`) for this to decide correctly.
 */
@Composable
internal fun ChoiceRows(
    spacing: Dp,
    perRowWhenWrapped: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val gap = spacing.roundToPx()
        val oneRowWidth = placeables.sumOf { it.width } + gap * (placeables.size - 1).coerceAtLeast(0)
        val rows = if (oneRowWidth <= constraints.maxWidth) {
            listOf(placeables)
        } else {
            placeables.chunked(perRowWhenWrapped.coerceAtLeast(1))
        }
        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val height = rowHeights.sum() + gap * (rows.size - 1).coerceAtLeast(0)
        val width = constraints.maxWidth
        layout(width, height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            rows.forEachIndexed { r, row ->
                val rowWidth = row.sumOf { it.width } + gap * (row.size - 1)
                var x = ((width - rowWidth) / 2).coerceAtLeast(0)
                row.forEach { p ->
                    p.placeRelative(x, y + (rowHeights[r] - p.height) / 2)
                    x += p.width + gap
                }
                y += rowHeights[r] + gap
            }
        }
    }
}

/**
 * A word drawn big and on ONE line: the font steps down from [maxSize] until [text] fits the
 * width it is given. `competition` at the usual 64sp is ~420dp — wider than a phone — and used
 * to break onto a second line, splitting the word the kid is meant to read as one thing.
 */
@Composable
internal fun SingleLineFit(
    text: String,
    style: TextStyle,
    maxSize: TextUnit,
    modifier: Modifier = Modifier,
    minSize: TextUnit = MIN_FIT_SIZE,
    content: @Composable (TextUnit) -> Unit,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier = modifier) {
        val maxPx = constraints.maxWidth
        val size = remember(text, style, maxSize, minSize, maxPx) {
            var s = maxSize.value
            while (s > minSize.value &&
                measurer.measure(text, style.copy(fontSize = s.sp), softWrap = false, maxLines = 1)
                    .size.width > maxPx
            ) {
                s -= FIT_STEP
            }
            s.sp
        }
        content(size)
    }
}

private val MIN_FIT_SIZE = 28.sp
private const val FIT_STEP = 2f
