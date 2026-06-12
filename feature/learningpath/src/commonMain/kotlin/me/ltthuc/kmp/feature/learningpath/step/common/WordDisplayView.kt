package me.ltthuc.kmp.feature.learningpath.step.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.WordDisplay
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap

private const val TAG = "WordDisplayView"

/**
 * Renders the visual for [word], preferring an [WordDisplay.Image] variant (used for words
 * that have no good emoji) and falling back to a randomly-picked [WordDisplay.Emoji] variant.
 *
 * The emoji choice is stable per [seedKey] composition (defaults to `word.text`) — same screen
 * mount keeps the same emoji; navigating away and back picks again.
 *
 * Resolution order: bundled image (sized to [fontSize]) → emoji [Text] if the image is absent or
 * fails to load → nothing while a present image is still loading (avoids flashing the fallback).
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
internal fun WordDisplayView(
    word: LessonWord,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    seedKey: Any = word.text,
    fontWeight: FontWeight? = null,
) {
    val imagePath = remember(seedKey) {
        word.displays.filterIsInstance<WordDisplay.Image>().firstOrNull()?.path
    }
    val emojiPick = remember(seedKey) {
        word.displays.filterIsInstance<WordDisplay.Emoji>().randomOrNull()?.char
    }

    // null = still loading; Result captures success/failure so we can tell "loading" from "failed".
    val loadResult: Result<ImageBitmap>? = if (imagePath != null) {
        produceState<Result<ImageBitmap>?>(initialValue = null, imagePath) {
            // Decode off the Main recompose dispatcher — decodeToImageBitmap() is synchronous CPU work.
            value = withContext(Dispatchers.Default) {
                runCatching { Res.readBytes(imagePath).decodeToImageBitmap() }
                    .onFailure { Napier.w(tag = TAG) { "No vocab image at $imagePath, falling back to emoji" } }
            }
        }.value
    } else {
        null
    }

    when {
        loadResult?.isSuccess == true -> {
            val sizeDp = with(LocalDensity.current) { fontSize.toDp() }
            Image(
                bitmap = loadResult.getOrThrow(),
                contentDescription = word.text,
                contentScale = ContentScale.Fit,
                modifier = modifier.size(sizeDp),
            )
        }

        (loadResult?.isFailure == true || imagePath == null) && emojiPick != null -> {
            Text(
                text = emojiPick,
                fontSize = fontSize,
                fontWeight = fontWeight ?: FontWeight.Normal,
                modifier = modifier,
            )
        }
        // else: a present image is still loading — render nothing to avoid flashing the emoji.
    }
}
