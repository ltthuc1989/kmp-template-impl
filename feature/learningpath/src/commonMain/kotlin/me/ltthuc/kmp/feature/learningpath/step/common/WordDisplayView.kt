package me.ltthuc.kmp.feature.learningpath.step.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ltthuc.kmp.core.content.ContentBytes
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.WordDisplay
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.koin.compose.koinInject

private const val TAG = "WordDisplayView"

/**
 * Renders the visual for [word], preferring an [WordDisplay.Image] variant (used for words
 * that have no good emoji) and falling back to a randomly-picked [WordDisplay.Emoji] variant.
 *
 * The emoji choice is stable per [seedKey] composition (defaults to `word.text`) — same screen
 * mount keeps the same emoji; navigating away and back picks again.
 *
 * Resolution order: image (sized to [fontSize]) → emoji [Text] if the image is absent or fails to
 * load → nothing while a present image is still loading (avoids flashing the fallback).
 */
@Composable
internal fun WordDisplayView(
    word: LessonWord,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    seedKey: Any = word.text,
    fontWeight: FontWeight? = null,
) {
    when (val art = wordArt(word, seedKey)) {
        is WordArt.Picture -> {
            val sizeDp = with(LocalDensity.current) { fontSize.toDp() }
            Image(
                bitmap = art.bitmap,
                contentDescription = word.text,
                contentScale = ContentScale.Fit,
                modifier = modifier.size(sizeDp),
            )
        }

        is WordArt.Emoji -> {
            Text(
                text = art.char,
                fontSize = fontSize,
                fontWeight = fontWeight ?: FontWeight.Normal,
                modifier = modifier,
            )
        }

        WordArt.Loading, WordArt.None -> Unit
    }
}

/**
 * Cùng nội dung với [WordDisplayView] nhưng ảnh ĐƯỢC PHÓNG cho vừa hết ô nó đứng, thay vì
 * vẽ trong một hình vuông cạnh bằng `fontSize`.
 *
 * Vì sao cần: ảnh vocab là ảnh vuông 256×256, [ContentScale.Fit] không cắt gì cả — nên cỡ
 * vẽ đúng bằng cạnh `fontSize` truyền vào. Một `68.sp` đặt giữa thẻ cao 120dp nghĩa là ảnh
 * chỉ chiếm 57% thẻ, phần còn lại là nền trống. Ở đây ảnh ăn trọn ô (Fit nên không méo,
 * không tràn), KHÔNG đụng gì tới cỡ thẻ.
 *
 * Emoji thì vẫn phải chừa: nó là chữ, hộp dòng cao hơn cỡ chữ ~1.17 lần, đặt đúng bằng cạnh
 * ô là lòi ra ngoài — nên emoji lấy [emojiFillRatio] cạnh ngắn.
 */
@Composable
internal fun FillingWordDisplayView(
    word: LessonWord,
    modifier: Modifier = Modifier,
    emojiFillRatio: Float = EMOJI_FILL_RATIO,
    seedKey: Any = word.text,
) {
    when (val art = wordArt(word, seedKey)) {
        is WordArt.Picture -> {
            Image(
                bitmap = art.bitmap,
                contentDescription = word.text,
                contentScale = ContentScale.Fit,
                modifier = modifier.fillMaxSize(),
            )
        }

        is WordArt.Emoji -> {
            BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
                val side = if (constraints.hasBoundedHeight) minOf(maxWidth, maxHeight) else maxWidth
                Text(text = art.char, fontSize = (side.value * emojiFillRatio).sp)
            }
        }

        WordArt.Loading, WordArt.None -> Unit
    }
}

/** Phần cạnh ngắn của ô mà emoji chiếm; phần chừa lại là hộp dòng của chữ. */
private const val EMOJI_FILL_RATIO = 0.78f

private sealed interface WordArt {
    /** Ảnh có khai báo nhưng chưa giải mã xong — vẽ trống, đừng nháy emoji rồi đổi. */
    data object Loading : WordArt
    data object None : WordArt
    data class Picture(val bitmap: ImageBitmap) : WordArt
    data class Emoji(val char: String) : WordArt
}

/**
 * Nguồn hình của [word]: ưu tiên [WordDisplay.Image] (dành cho từ không có emoji nào tả đúng),
 * không có thì bốc ngẫu nhiên một [WordDisplay.Emoji]. Emoji cố định theo [seedKey] — cùng một
 * lần dựng màn thì giữ nguyên, rời màn rồi quay lại mới bốc lại.
 */
@Composable
private fun wordArt(word: LessonWord, seedKey: Any): WordArt {
    val contentBytes: ContentBytes = koinInject()
    val imagePath = remember(seedKey) {
        word.displays.filterIsInstance<WordDisplay.Image>().firstOrNull()?.path
    }
    val emojiPick = remember(seedKey) {
        word.displays.filterIsInstance<WordDisplay.Emoji>().randomOrNull()?.char
    }

    // null = still loading; Result captures success/failure so we can tell "loading" from "failed".
    val loadResult: Result<ImageBitmap>? = if (imagePath != null) {
        produceState<Result<ImageBitmap>?>(initialValue = null, imagePath) {
            // Vocab art ships in the app today, but it is read through ContentBytes like every
            // other image so that moving any of it into a content pack stays a data change.
            // Decode off the Main recompose dispatcher — decodeToImageBitmap() is synchronous CPU work.
            value = withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = contentBytes.load(imagePath) ?: error("no bytes for $imagePath")
                    bytes.decodeToImageBitmap()
                }.onFailure { Napier.w(tag = TAG) { "No vocab image at $imagePath, falling back to emoji" } }
            }
        }.value
    } else {
        null
    }

    val bitmap = loadResult?.getOrNull()
    return when {
        bitmap != null -> WordArt.Picture(bitmap)
        (loadResult?.isFailure == true || imagePath == null) && emojiPick != null ->
            WordArt.Emoji(emojiPick)
        imagePath != null && loadResult == null -> WordArt.Loading
        else -> WordArt.None
    }
}
