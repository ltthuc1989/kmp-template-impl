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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark

/**
 * Choice for FillLetter: a letter or a chunk (`th`, `spr`, `o_e`). White fill + pastel-tint ring +
 * dark teal text.
 *
 * [longestLabel] is the longest label of the WHOLE round, so all four choices share one shape and
 * text size: a round of single letters keeps the round circles; any longer round turns every choice
 * into a pill with the same height, and the text steps down so four 3-letter pills still fit one row
 * on a 360dp phone.
 *
 * [shakeKey] increments to trigger a wobble when this choice is the wrong pick.
 */
@Composable
internal fun ChunkChoice(
    label: String,
    longestLabel: Int,
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
    val single = longestLabel <= 1
    val shape = if (single) CircleShape else RoundedCornerShape(CHOICE_SIZE / 2)
    val sizing = if (single) {
        Modifier.size(CHOICE_SIZE)
    } else {
        Modifier.height(CHOICE_SIZE).widthIn(min = CHOICE_SIZE)
    }

    Box(
        modifier = modifier
            .then(sizing)
            .rotate(rotation.value)
            .clip(shape)
            .background(Color.White)
            .border(width = 3.dp, color = tint, shape = shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = if (single) 0.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontFamily = LocalPhonicsFontFamily.current,
            fontSize = choiceFontSize(longestLabel),
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}

/** Spacing between choices — tighter once labels are long, so four still fit one row. */
internal fun choiceSpacing(longestLabel: Int) = if (longestLabel <= 2) 14.dp else 8.dp

private fun choiceFontSize(longestLabel: Int): TextUnit = when {
    longestLabel <= 2 -> 36.sp
    longestLabel == 3 -> 28.sp
    else -> 24.sp
}

private val CHOICE_SIZE = 72.dp

/**
 * Big word display with the answer hidden. Each hidden span shows as ONE `_` whatever its length
 * (chốt D1 2026-09-17): `wa_` for `tch`, so the kid can't rule choices out by counting letters.
 * Magic-e has two spans → `h_m_`.
 */
@Composable
internal fun WordWithBlank(
    word: String,
    blankSpans: ImmutableList<IntRange>,
    isFilled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (isFilled) word else maskWord(word, blankSpans),
            fontFamily = LocalPhonicsFontFamily.current,
            fontSize = 64.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ReadingTextDark,
        )
    }
}

/** `father` + [2..3] → `fa_er`. Spans outside the word are ignored rather than crashing. */
internal fun maskWord(word: String, blankSpans: List<IntRange>): String = buildString {
    var cursor = 0
    for (span in blankSpans.sortedBy { it.first }) {
        if (span.first < cursor || span.last >= word.length) continue
        append(word, cursor, span.first)
        append('_')
        cursor = span.last + 1
    }
    append(word, cursor, word.length)
}
