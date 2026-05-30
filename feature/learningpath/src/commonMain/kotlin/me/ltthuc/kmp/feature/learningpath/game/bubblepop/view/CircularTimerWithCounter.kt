package me.ltthuc.kmp.feature.learningpath.game.bubblepop.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Top-left widget for BubblePop's 30s round: circular progress ring counts down time,
 * with the kid's current target-pop count rendered in the middle (e.g. "3/10").
 *
 * Progress = `timeRemainingMs / totalMs` — full ring at start, empty when timer expires.
 * Color stays primary throughout (no red-shift when low) to avoid scaring 3-8yo.
 */
@Composable
internal fun CircularTimerWithCounter(
    timeRemainingMs: Long,
    totalMs: Long,
    popped: Int,
    targetPool: Int,
    modifier: Modifier = Modifier,
) {
    val progress = if (totalMs > 0) {
        (timeRemainingMs.toFloat() / totalMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(72.dp),
            color = primary,
            strokeWidth = 6.dp,
            trackColor = primary.copy(alpha = 0.18f),
        )
        Text(
            text = "$popped/$targetPool",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = primary,
            maxLines = 1,
        )
    }
}
