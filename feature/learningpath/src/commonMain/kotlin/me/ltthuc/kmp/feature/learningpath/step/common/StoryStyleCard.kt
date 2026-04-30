package me.ltthuc.kmp.feature.learningpath.step.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 3-layer "puffy comic-frame" card matching the visual used by Story step:
 * outer rounded shell with soft shadow + colored border, inner tinted panel.
 * Reused by Sound Intro / Chant / Vocabulary so the first 3 step screens share
 * the same visual chrome around their content.
 *
 * @param aspectRatio when non-null, the card enforces this ratio (e.g. 4f/3f
 *  for image stages). Pass null to wrap content height — useful for cards whose
 *  inner content has variable height (chant tokens, vocabulary controls).
 */
@Composable
internal fun StoryStyleCard(
    modifier: Modifier = Modifier,
    aspectRatio: Float? = 4f / 3f,
    content: @Composable BoxScope.() -> Unit,
) {
    val frameColor = MaterialTheme.colorScheme.primaryContainer
    val innerBg = frameColor.copy(alpha = INNER_BG_ALPHA)
    val outerShape = RoundedCornerShape(STORY_OUTER_CORNER_DP.dp)
    val innerShape = RoundedCornerShape(STORY_INNER_CORNER_DP.dp)

    val sized = if (aspectRatio != null) {
        modifier.fillMaxWidth().aspectRatio(aspectRatio)
    } else {
        modifier.fillMaxWidth()
    }

    Box(
        modifier = sized
            .shadow(
                elevation = SHADOW_ELEVATION_DP.dp,
                shape = outerShape,
                ambientColor = Color.Black.copy(alpha = SHADOW_AMBIENT_ALPHA),
                spotColor = Color.Black.copy(alpha = SHADOW_SPOT_ALPHA),
            )
            .clip(outerShape)
            .background(Color.White)
            .border(
                width = STORY_BORDER_WIDTH_DP.dp,
                color = frameColor.copy(alpha = BORDER_ALPHA),
                shape = outerShape,
            )
            .padding(STORY_FRAME_PADDING_DP.dp),
    ) {
        Box(
            modifier = (if (aspectRatio != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .clip(innerShape)
                .background(innerBg),
            content = content,
        )
    }
}

internal const val STORY_OUTER_CORNER_DP = 32
internal const val STORY_INNER_CORNER_DP = 16
internal const val STORY_BORDER_WIDTH_DP = 8
internal const val STORY_FRAME_PADDING_DP = 16
private const val SHADOW_ELEVATION_DP = 6
private const val SHADOW_AMBIENT_ALPHA = 0.08f
private const val SHADOW_SPOT_ALPHA = 0.12f
private const val BORDER_ALPHA = 0.55f
private const val INNER_BG_ALPHA = 0.15f
