package me.ltthuc.kmp.feature.learningpath.game.pickword.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView

/**
 * Pastel pill-shaped word card used as a tap-to-choose option in PickWord (and reusable
 * for similar pick-style games). Soft pastel tint behind dark teal text reads cleanly
 * against the cream play area.
 *
 * [shakeKey] increments to trigger a wobble — used when the user taps the wrong choice.
 */
@Composable
internal fun PickWordChoice(
    word: String,
    tint: Color,
    enabled: Boolean,
    shakeKey: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            rotation.snapTo(0f)
            rotation.animateTo(-7f, tween(60))
            rotation.animateTo(7f, tween(90))
            rotation.animateTo(-4f, tween(80))
            rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(32.dp)

    Box(
        modifier = modifier
            .rotate(rotation.value)
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(tint, tint.copy(alpha = 0.85f)),
                ),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            fontFamily = LocalPhonicsFontFamily.current,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
        )
    }
}

/**
 * Picture card showing the target word's referent. White rounded card with a soft shadow —
 * focal point of each PickWord / FillLetter / SpellLetters round.
 *
 * Nhận cả [LessonWord] chứ không nhận chuỗi emoji: 35 từ trên cả 5 cấp có ẢNH WebP vẽ
 * riêng vì emoji của chúng gây hiểu nhầm — `cave` mà lấy emoji thì ra 🕳️ cái hố, `nail`
 * ra 🔨 cái búa, `tail` ra 🐈 cả con mèo, còn `rag` với `mop` DÙNG CHUNG 🧹 nên bé không
 * thể phân biệt. [WordDisplayView] ưu tiên ảnh, không có ảnh mới rơi về emoji.
 *
 * Cỡ suy từ bề ngang thật của thẻ chứ không để hằng số 120sp: thẻ do chỗ gọi định cỡ
 * (`fillMaxWidth(0.40f)`..`0.45f`), nên hằng số cố định thì máy nhỏ tràn, máy to hụt.
 */
@Composable
internal fun PicturePanel(
    word: LessonWord?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = shape)
            .clip(shape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        if (word == null) {
            Text(text = "❓", fontSize = (maxWidth.value * PICTURE_FILL_RATIO).sp)
        } else {
            WordDisplayView(word = word, fontSize = (maxWidth.value * PICTURE_FILL_RATIO).sp)
        }
    }
}

/** Phần bề ngang thẻ mà hình chiếm; chừa lại mép cho bóng đổ và góc bo. */
private const val PICTURE_FILL_RATIO = 0.72f

/**
 * Empty slot indicator below the picture. After kid picks correctly the answered word
 * is rendered inside; before that, a dashed-style outline placeholder shows where the
 * answer belongs.
 */
@Composable
internal fun AnswerSlot(
    filledWord: String?,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (filledWord != null) {
                    Color(0xFFE8F7E0)
                } else {
                    Color(0xFFEEEEEE)
                },
            )
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (filledWord != null) {
                Text(
                    text = filledWord,
                    fontFamily = LocalPhonicsFontFamily.current,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ReadingTextDark,
                )
            }
        }
    }
}
