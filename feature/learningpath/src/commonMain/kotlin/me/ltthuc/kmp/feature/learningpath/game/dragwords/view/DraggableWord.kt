package me.ltthuc.kmp.feature.learningpath.game.dragwords.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark

/**
 * Draggable word tile in DragWords. Renders the word inside a pastel pill; the user can
 * grab and drag it anywhere on the screen. The parent decides what to do on release via
 * [onDragEnd] — it receives the tile's current absolute center in window coordinates and
 * returns `true` if the drop succeeded (tile stays at the dropped offset / morphs to slot)
 * or `false` (animate back to origin).
 *
 * `Animatable<Offset>` drives the visual offset; on each drag frame we accumulate dx/dy
 * into it. On drag end we suspend until the parent's match/snap-back animation completes.
 */
@Composable
internal fun DraggableWord(
    word: String,
    tint: Color,
    isUsed: Boolean,
    snapTarget: Offset?, // non-null after correct match: snap to this absolute window center
    onCenterPositioned: (Offset) -> Unit,
    onDragEnd: (currentCenter: Offset) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val dragOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    var originCenter by remember { mutableStateOf(Offset.Zero) }

    // When the matched target is known, animate offset so tile center lands at the slot.
    LaunchedEffect(snapTarget) {
        snapTarget?.let { target ->
            val delta = target - originCenter
            dragOffset.animateTo(delta, animationSpec = tween(220))
        }
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
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
            .shadow(elevation = if (scale.value > 1.02f) 12.dp else 4.dp, shape = shape)
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
                        scope.launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        if (!matched) {
                            scope.launch {
                                dragOffset.animateTo(
                                    Offset.Zero,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        }
                        // Matched: snap handled via LaunchedEffect(snapTarget)
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
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = word,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
        )
    }
}
