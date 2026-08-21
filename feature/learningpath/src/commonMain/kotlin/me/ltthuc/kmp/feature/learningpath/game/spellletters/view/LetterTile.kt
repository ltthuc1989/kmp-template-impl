package me.ltthuc.kmp.feature.learningpath.game.spellletters.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.game.common.SlotOutline

/**
 * Draggable letter tile in SpellLetters. Pattern cloned from `DraggableWord` in DragWords.
 *
 * Kid presses and drags; [onDragEnd] is called on release with the tile's current absolute
 * center (window coords) and returns true if the drop matched a correct slot. If `false`,
 * the tile springs back to its scatter origin via Animatable; if `true`, the parent provides
 * a [snapTarget] in a later recomposition to land the tile precisely on the slot center,
 * after which it fades out ([isUsed] = true) — the slot itself takes over rendering the letter.
 */
@Composable
internal fun DraggableLetterTile(
    letter: Char,
    tint: Color,
    isUsed: Boolean,
    snapTarget: Offset?,
    onCenterPositioned: (Offset) -> Unit,
    onDragEnd: (currentCenter: Offset) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var originCenter by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(snapTarget) {
        snapTarget?.let { target ->
            val delta = target - originCenter
            dragOffset.animateTo(delta, animationSpec = tween(220))
        }
    }

    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(72.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                val center = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                if (center != originCenter) {
                    originCenter = center
                    onCenterPositioned(center)
                }
            }
            .offset { IntOffset(dragOffset.value.x.toInt(), dragOffset.value.y.toInt()) }
            .scale(scale.value)
            .alpha(if (isUsed) 0f else 1f)
            .shadow(elevation = if (scale.value > 1.02f) 10.dp else 4.dp, shape = shape)
            .clip(shape)
            .background(tint)
            .pointerInput(isUsed, originCenter) {
                if (isUsed) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        scope.launch { scale.animateTo(1.1f, tween(120)) }
                    },
                    onDragEnd = {
                        val center = originCenter + dragOffset.value
                        val matched = onDragEnd(center)
                        scope.launch {
                            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                        if (!matched) {
                            scope.launch {
                                dragOffset.animateTo(
                                    Offset.Zero,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { scale.animateTo(1f, tween(120)) }
                        scope.launch { dragOffset.animateTo(Offset.Zero, tween(180)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            dragOffset.snapTo(dragOffset.value + Offset(dragAmount.x, dragAmount.y))
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontFamily = LocalPhonicsFontFamily.current,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
        )
    }
}

/**
 * Slot in the target-word row. Shows the letter when [filled] is true; otherwise a tinted
 * placeholder. Reports its center via [onCenterPositioned] so the drag canvas can match
 * dropped tiles to the nearest slot.
 */
@Composable
internal fun WordSlot(
    letter: Char,
    filled: Boolean,
    onCenterPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    var lastReportedCenter by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .size(72.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                val center = Offset(pos.x + size.width / 2f, pos.y + size.height / 2f)
                if (center != lastReportedCenter) {
                    lastReportedCenter = center
                    onCenterPositioned(center)
                }
            }
            .clip(shape)
            .background(if (filled) Color(0xFFE8F7E0) else Color.Transparent)
            .border(width = 3.dp, color = SlotOutline.copy(alpha = 0.8f), shape = shape),
        contentAlignment = Alignment.Center,
    ) {
        if (filled) {
            Text(
                text = letter.toString(),
                fontFamily = LocalPhonicsFontFamily.current,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ReadingTextDark,
            )
        }
    }
}
