package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import me.ltthuc.kmp.core.content.ContentManifestLoader
import me.ltthuc.kmp.core.content.ContentPackDownloader
import me.ltthuc.kmp.core.content.DownloadProgress
import me.ltthuc.kmp.core.content.PackStore

/** What the app knows about one unit's content pack right now. */
sealed interface PackState {
    /** Ships inside the app — there is nothing to download, ever. */
    data object Bundled : PackState

    data class NotDownloaded(val bytes: Long) : PackState

    data class Downloading(val progress: DownloadProgress) : PackState

    data object Ready : PackState

    data class Failed(val cause: Throwable, val bytes: Long) : PackState
}

/**
 * Pack availability for the UI: which units still need downloading, how big they are, and
 * how a download is going.
 *
 * Access is *not* decided here — callers ask [LevelAccess]. Keeping the two apart is what
 * lets a level be opened by a purchase today and by a rewarded ad later without this class
 * changing at all.
 */
class ContentPackRepository(
    private val manifestLoader: ContentManifestLoader,
    private val downloader: ContentPackDownloader,
    private val packStore: PackStore,
) {
    private val states = MutableStateFlow<Map<String, PackState>>(emptyMap())

    /** Live state per pack id, e.g. `"L1U3"`. Absent means "not looked at yet". */
    val packStates: StateFlow<Map<String, PackState>> = states.asStateFlow()

    /** Packs belonging to [levelId], in curriculum order — `["L1U3", "L1U4", …]`. */
    suspend fun packIdsForLevel(levelId: String): List<String> =
        manifestLoader.load()
            .downloadablePackIds()
            .filter { it.startsWith("${levelId}U") }
            .sortedBy { it.substringAfter("U").toIntOrNull() ?: 0 }

    /** Bytes still missing across [levelId] — what a "download 8.9 MB?" prompt should show. */
    suspend fun pendingBytesForLevel(levelId: String): Long =
        packIdsForLevel(levelId).sumOf { downloader.pendingBytes(it) }

    /** Bytes currently stored on disk for [levelId], for the storage screen. */
    suspend fun storedBytesForLevel(levelId: String): Long {
        val manifest = manifestLoader.load()
        return packIdsForLevel(levelId).sumOf { packId ->
            packStore.sizeOf(manifest.assetsInPack(packId).values.map { it.hash })
        }
    }

    suspend fun refresh(packId: String): PackState {
        val manifest = manifestLoader.load()
        val state = when {
            manifest.isPackBundled(packId) -> PackState.Bundled
            else -> {
                val pending = downloader.pendingBytes(packId)
                if (pending == 0L) PackState.Ready else PackState.NotDownloaded(pending)
            }
        }
        publish(packId, state)
        return state
    }

    /**
     * Downloads [packId], emitting progress. Collect it to drive a progress bar; cancelling
     * the collection stops the download and leaves finished files in place, so resuming
     * later only fetches what is still missing.
     */
    fun download(packId: String): Flow<DownloadProgress> =
        downloader.download(packId)
            .onEach { publish(packId, PackState.Downloading(it)) }
            .onCompletion { cause ->
                when (cause) {
                    null -> publish(packId, PackState.Ready)
                    // Cancellation is a user leaving the screen, not a failure: keep the
                    // last known progress so returning shows how far it got.
                    is kotlin.coroutines.cancellation.CancellationException -> Unit
                    else -> {
                        Napier.e("Pack $packId download failed", cause)
                        publish(packId, PackState.Failed(cause, downloader.pendingBytes(packId)))
                    }
                }
            }

    /** Frees a pack's files. Access is untouched — the unit stays unlocked, just re-downloadable. */
    suspend fun delete(packId: String) {
        val manifest = manifestLoader.load()
        packStore.delete(manifest.assetsInPack(packId).values.map { it.hash })
        refresh(packId)
    }

    /**
     * Sweeps files no longer referenced by the manifest — what a content update leaves
     * behind once an asset's bytes, and therefore its hash, changed.
     */
    suspend fun sweepStaleFiles() {
        val keep = manifestLoader.load().assets.values.mapTo(mutableSetOf()) { it.hash }
        packStore.deleteUnreferenced(keep)
    }

    private fun publish(packId: String, state: PackState) {
        states.value = states.value + (packId to state)
    }
}
