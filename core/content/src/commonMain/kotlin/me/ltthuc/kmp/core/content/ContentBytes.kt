package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Reads an asset's bytes wherever it currently lives.
 *
 * Audio never needs this — the players take a URI or a file path, so bytes would just be
 * a wasted copy. Images do: `decodeToImageBitmap()` needs the bytes in hand, and they must
 * arrive the same way whether the file ships in the app or was downloaded.
 *
 * Returns null instead of throwing. Both image call sites already fall back to an emoji,
 * which is the right outcome for a child who is offline and has not downloaded a pack.
 */
class ContentBytes(
    private val locator: AssetLocator,
    private val downloader: ContentPackDownloader,
) {
    /**
     * [path] may be a logical path (`images/vocab/belt.webp`) or the composeResources form
     * with the `files/` prefix, which is how paths are written in `curriculum.json` and
     * built in `StoryRepository`. Both are accepted so that content data does not have to
     * be rewritten to move an asset out of the app.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(path: String): ByteArray? {
        val logicalPath = path.removePrefix(RESOURCE_PREFIX)

        return when (val source = locator.resolve(logicalPath)) {
            is AssetSource.Bundled -> read("$RESOURCE_PREFIX$logicalPath") { Res.readBytes(it) }
            is AssetSource.Local -> read(source.path) { readFileBytes(it) }
            is AssetSource.Remote -> read(logicalPath) {
                readFileBytes(downloader.fetchOne(it, source.asset))
            }
            AssetSource.Missing -> null
        }
    }

    private suspend fun read(target: String, block: suspend (String) -> ByteArray): ByteArray? =
        runCatching { block(target) }
            .onFailure { Napier.w("Unable to read content at $target: ${it.message}") }
            .getOrNull()

    private companion object {
        const val RESOURCE_PREFIX = "files/"
    }
}

/** Reads a file the app itself downloaded. Paths come from [PackStore], never from user input. */
internal expect suspend fun readFileBytes(path: String): ByteArray
