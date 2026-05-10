package me.ltthuc.kmp.feature.learningpath.step.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.WordDisplay

/**
 * Renders one randomly-picked [WordDisplay.Emoji] variant for [word]. The choice is
 * stable per [seedKey] composition (defaults to `word.text`) — same screen mount keeps
 * the same emoji; navigating away and back picks again.
 *
 * If [word] has no Emoji variants (only Image, or empty) the composable renders nothing.
 * Image variants are skipped until image rendering lands in a follow-up task.
 */
@Composable
internal fun WordDisplayView(
    word: LessonWord,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    seedKey: Any = word.text,
    fontWeight: FontWeight? = null,
) {
    val emojiVariants = remember(seedKey) {
        word.displays.filterIsInstance<WordDisplay.Emoji>()
    }
    val pick = remember(seedKey) { emojiVariants.randomOrNull() }
    if (pick != null) {
        Text(
            text = pick.char,
            fontSize = fontSize,
            fontWeight = fontWeight ?: FontWeight.Normal,
            modifier = modifier,
        )
    }
}
