package me.ltthuc.kmp.feature.learningpath.game.filletter.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark

/**
 * Letter-choice circle for FillLetter game. Tappable circle with single letter inside.
 * Visual: white fill + pastel-tint ring + dark teal letter.
 *
 * [shakeKey] increments to trigger a wobble when this letter is the wrong pick.
 */
@Composable
internal fun LetterCircle(
    letter: Char,
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

    Box(
        modifier = modifier
            .size(72.dp)
            .rotate(rotation.value)
            .clip(CircleShape)
            .background(Color.White)
            .border(width = 3.dp, color = tint, shape = CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.toString(),
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
        )
    }
}

/**
 * Big word display with one character potentially shown as a blank placeholder. Each
 * non-blank character renders dark; the blank renders as a tinted underscore-like rect.
 */
@Composable
internal fun WordWithBlank(
    word: String,
    blankIndex: Int,
    isFilled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Simple text display: blank shown as "_" — UI complexity not needed for v1.
        val display = if (isFilled) {
            word
        } else {
            word.mapIndexed { i, c -> if (i == blankIndex) '_' else c }.joinToString("")
        }
        Text(
            text = display,
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
        )
    }
}
