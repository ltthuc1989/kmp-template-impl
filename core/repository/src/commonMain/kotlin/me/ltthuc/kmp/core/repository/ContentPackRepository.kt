package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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

    /**
     * [retryable] separates "the connection dropped" from "the device is full" or "the file
     * is not on the CDN". Only the first is fixed by tapping again; offering a retry button
     * for the others sends the user in a loop.
     */
    data class Failed(val cause: Throwable, val bytes: Long, val retryable: Boolean) : PackState
}

private const val HTTP_NOT_FOUND = "HTTP 404"

/** Walks the cause chain — the downloader wraps the real failure after its last attempt. */
internal fun isRetryableFailure(cause: Throwable?): Boolean {
    var current = cause
    while (current != null) {
        val message = current.message.orEmpty()
        if (message.contains("No space left", ignoreCase = true) ||
            message.contains("ENOSPC", ignoreCase = true) ||
            message.contains(HTTP_NOT_FOUND)
        ) {
            return false
        }
        current = current.cause
    }
    return true
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
    // The repository owns this scope on purpose: a background fill must outlive the screen
    // that started it, so the rest of a level keeps arriving while the child is already in a
    // lesson. A screen-scoped coroutine would die at the first navigation.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var backgroundJob: Job? = null

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
                        publish(
                            packId,
                            PackState.Failed(
                                cause = cause,
                                bytes = downloader.pendingBytes(packId),
                                retryable = isRetryableFailure(cause),
                            ),
                        )
                    }
                }
            }

    /**
     * Quietly fetches whatever [levelId] is still missing, one pack at a time, after the unit
     * the child actually tapped is already playing. Failures stay silent — this is a
     * head start, not something anyone is waiting on; a unit that misses out simply shows
     * its download badge when it is reached.
     */
    fun downloadLevelInBackground(levelId: String) {
        if (backgroundJob?.isActive == true) return
        backgroundJob = scope.launch {
            for (packId in packIdsForLevel(levelId)) {
                if (refresh(packId) !is PackState.NotDownloaded) continue
                runCatching { download(packId).collect { } }
            }
        }
    }

    /** Frees a pack's files. Access is untouched — the unit stays unlocked, just re-downloadable. */
    suspend fun delete(packId: String) {
        val manifest = manifestLoader.load()
        packStore.delete(manifest.assetsInPack(packId).values.map { it.hash })
        refresh(packId)
    }

    /** Drops every downloaded pack. QA tool: puts the device back to a just-installed state. */
    suspend fun deleteAll() {
        backgroundJob?.cancel()
        packStore.clear()
        manifestLoader.load().downloadablePackIds().forEach { refresh(it) }
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
