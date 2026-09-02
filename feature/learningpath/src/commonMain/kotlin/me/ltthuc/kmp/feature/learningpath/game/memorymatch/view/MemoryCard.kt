package me.ltthuc.kmp.feature.learningpath.game.memorymatch.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView

/**
 * Memory match card with a 3D Y-axis flip between face-down and face-up states.
 *
 * The flip is driven by [animateFloatAsState] reacting to [isFaceUp]; we use
 * `graphicsLayer rotationY` + a generous `cameraDistance` so the swap looks like a real
 * card pivot. To avoid the back-face being mirrored when rotation crosses 90°, we render
 * only one side at a time and counter-rotate the visible side past the midpoint.
 *
 * - [tint] is the pair colour (both halves of a pair share the same tint) so after both
 *   are matched the kid sees them as a colour-coded set.
 * - [isMatched] adds a subtle green outer glow so completed pairs read as "stuck".
 */
@Composable
internal fun MemoryCard(
    letter: String,
    tint: Color,
    isFaceUp: Boolean,
    isMatched: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    picture: LessonWord? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFaceUp) 180f else 0f,
        animationSpec = tween(durationMillis = FLIP_DURATION_MS),
        label = "memoryCardFlip",
    )
    val density = LocalDensity.current
    val cameraDistancePx = with(density) { 12.dp.toPx() } * 8

    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isMatched) 12.dp else 6.dp,
                shape = shape,
                ambientColor = if (isMatched) Color(0xFF2EE6A0) else Color.Black,
                spotColor = if (isMatched) Color(0xFF2EE6A0) else Color.Black,
            )
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled && !isFaceUp,
                onClick = onTap,
            )
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = cameraDistancePx
            },
        contentAlignment = Alignment.Center,
    ) {
        if (rotation <= 90f) {
            CardBackFace()
        } else {
            CardFrontFace(
                letter = letter,
                tint = tint,
                isMatched = isMatched,
                modifier = Modifier.graphicsLayer { rotationY = 180f },
                picture = picture,
            )
        }
    }
}

@Composable
private fun CardBackFace(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(brush = Brush.linearGradient(BACK_GRADIENT)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.65f),
        )
    }
}

@Composable
private fun CardFrontFace(
    letter: String,
    tint: Color,
    isMatched: Boolean,
    modifier: Modifier = Modifier,
    picture: LessonWord? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = 0.95f),
                        Color.White.copy(alpha = 0.85f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (picture != null) {
            // Thẻ HÌNH: phủ gần kín mặt thẻ, cỡ suy từ bề ngang thật của thẻ chứ không
            // để một hằng số sp. Trước đây emoji đi chung nhánh chữ bên dưới và ăn đúng
            // 52sp — vì `"🍎".length` là 2 (cặp surrogate), không phải vì ai chọn cỡ đó —
            // nên hình bé tí giữa thẻ ~153dp. Đi qua [WordDisplayView] còn được thêm ảnh
            // WebP của những từ đã vẽ riêng, thay vì luôn rơi về emoji.
            WordDisplayView(
                word = picture,
                fontSize = (maxWidth.value * PICTURE_FILL_RATIO).sp,
                modifier = Modifier.padding(CARD_PADDING_DP.dp),
            )
        } else {
            // Mặt thẻ chữ: chữ cái ("A"), vần ("am") hoặc TỪ ("ram"). Cỡ chữ phải suy từ
            // độ dài, không để cố định 64sp — từ 3 ký tự ở cỡ đó tràn khỏi thẻ vuông,
            // đúng lỗi đã gặp ở bong bóng.
            val fontSizeSp = when {
                letter.length <= 1 -> 64f
                letter.length == 2 -> 52f
                else -> (150f / letter.length).coerceAtLeast(24f)
            }
            Text(
                text = letter,
                fontFamily = LocalPhonicsFontFamily.current,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.1f).sp,
                maxLines = 1,
                softWrap = false,
                fontWeight = FontWeight.Black,
                color = if (isMatched) Color(0xFF0E5562) else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Phần bề ngang thẻ mà hình chiếm. 0.78 chứ không phải 1.0: `WordDisplayView` dựng ảnh
 * VUÔNG cạnh bằng `fontSize`, mà thẻ cũng vuông — để 1.0 thì ảnh chạm sát mép bo tròn và
 * bị góc bo cắt mất bốn chân.
 */
private const val PICTURE_FILL_RATIO = 0.78f
private const val CARD_PADDING_DP = 6

private val BACK_GRADIENT = listOf(
    Color(0xFF0E7C8A),
    Color(0xFF1FA3B8),
)

private const val FLIP_DURATION_MS = 350
