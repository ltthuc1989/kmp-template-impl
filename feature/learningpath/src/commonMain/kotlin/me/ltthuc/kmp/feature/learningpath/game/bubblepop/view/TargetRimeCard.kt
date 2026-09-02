package me.ltthuc.kmp.feature.learningpath.game.bubblepop.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily

/**
 * Thẻ hiện VẦN mục tiêu ra giữa màn, một lượt, ngay trước khi bong bóng nổi lên.
 *
 * Vì sao màn này cần nhìn chứ không chỉ nghe: từ cấp 3, các vần trong cùng một unit đọc
 * lên giống hệt nhau — `ai` với `ay` đều /eɪ/, `ee` `ea` `y` `ey` đều /iː/. Tai không tách
 * được, đó là sự thật ngữ âm chứ không phải lỗi bản thu. Mọi giáo trình dạy `ai`/`ay` đều
 * là word sort có nhãn cột hiện sẵn, không phải bài nghe.
 *
 * Thẻ chỉ hiện rồi ẩn — để suốt vòng thì bé chỉ việc dò chữ, không phải nhớ gì.
 *
 * Chữ to 72sp và **luôn chữ thường**: phonics dạy vần bằng chữ thường, và bé phải thấy
 * đúng hình dạng sẽ gặp trên bong bóng.
 */
@Composable
internal fun TargetRimeCard(
    rime: String,
    modifier: Modifier = Modifier,
) {
    // Nảy nhẹ khi xuất hiện để mắt bắt được ngay giữa một màn đang động.
    val scale = remember(rime) { Animatable(0.72f) }
    LaunchedEffect(rime) {
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Box(
        modifier = modifier
            .scale(scale.value)
            .shadow(elevation = 16.dp, shape = CARD_SHAPE)
            .clip(CARD_SHAPE)
            .background(Color.White.copy(alpha = 0.94f))
            .padding(horizontal = 36.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = rime.lowercase(),
            fontFamily = LocalPhonicsFontFamily.current,
            fontSize = RIME_FONT_SP.sp,
            lineHeight = (RIME_FONT_SP * 1.15f).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false,
            color = TARGET_RIME_COLOR,
        )
    }
}

private val CARD_SHAPE = RoundedCornerShape(28.dp)

/** Hồng của vần đang dạy — cùng màu màn ghép vần dùng, để bé nối được hai chỗ với nhau. */
private val TARGET_RIME_COLOR = Color(0xFFE6007E)

private const val RIME_FONT_SP = 72f
