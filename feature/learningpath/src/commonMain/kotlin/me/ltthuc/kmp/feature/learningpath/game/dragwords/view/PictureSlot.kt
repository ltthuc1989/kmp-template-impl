package me.ltthuc.kmp.feature.learningpath.game.dragwords.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.game.common.SlotOutline

/**
 * Picture with answer slot underneath. Reports the picture box's center to its parent via
 * [onCenterPositioned] so the drag-canvas can match dropped tiles to the nearest slot.
 *
 * Shakes when [shakeKey] increments (wrong drop landed on this picture).
 */
@Composable
internal fun PictureSlot(
    emoji: String,
    filledWord: String?,
    shakeKey: Int,
    onCenterPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(shakeKey) {
        if (shakeKey > 0) {
            rotation.snapTo(0f)
            rotation.animateTo(-6f, tween(50))
            rotation.animateTo(6f, tween(80))
            rotation.animateTo(-3f, tween(70))
            rotation.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
        }
    }
    val pictureShape = RoundedCornerShape(16.dp)
    val slotShape = RoundedCornerShape(12.dp)
    var lastReportedCenter by remember { androidx.compose.runtime.mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .rotate(rotation.value),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    val size = coords.size
                    val center = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                    if (center != lastReportedCenter) {
                        lastReportedCenter = center
                        onCenterPositioned(center)
                    }
                }
                .shadow(elevation = 4.dp, shape = pictureShape)
                .clip(pictureShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji.ifBlank { "❓" }, fontSize = 56.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(slotShape)
                .background(if (filledWord != null) Color(0xFFE8F7E0) else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = SlotOutline.copy(alpha = if (filledWord != null) 0f else 0.8f),
                    shape = slotShape,
                )
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (filledWord != null) {
                Text(
                    text = filledWord,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ReadingTextDark,
                )
            }
        }
    }
}
