package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.ltthuc.kmp.core.content.ContentPackDownloader
import me.ltthuc.kmp.core.content.DownloadProgress
import me.ltthuc.kmp.core.content.ManifestSource
import me.ltthuc.kmp.core.content.PackFiles
import me.ltthuc.kmp.core.content.PackIndex

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
 * Which stored hashes survive a sweep. Pure — extracted so the rule can be tested without a
 * filesystem, the way [isRetryableFailure] is.
 *
 * Everything the manifest currently points at, plus one carefully bounded extra: the copy an
 * asset was *previously* stored under, while its replacement is still missing. That single
 * exception is what keeps a finished unit audible offline right after an app update — see
 * [ContentPackRepository.sweepStaleFiles]. A predecessor stops qualifying the moment its
 * replacement lands, so the reprieve cannot turn into a leak.
 *
 * @param currentHashes logical path → hash the manifest wants today
 * @param indexed logical path → hash actually stored, from [me.ltthuc.kmp.core.content.PackIndex]
 */
internal fun hashesToKeep(
    currentHashes: Map<String, String>,
    indexed: Map<String, String>,
    isPresent: (String) -> Boolean,
): Set<String> {
    val keep = currentHashes.values.toMutableSet()
    for ((logicalPath, storedHash) in indexed) {
        val fresh = currentHashes[logicalPath] ?: continue // dropped from the curriculum
        if (storedHash != fresh && !isPresent(fresh)) keep += storedHash
    }
    return keep
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
    private val manifestSource: ManifestSource,
    private val downloader: ContentPackDownloader,
    private val packFiles: PackFiles,
    private val packIndex: PackIndex,
) {
    // The repository owns this scope on purpose: a background fill must outlive the screen
    // that started it, so the rest of a level keeps arriving while the child is already in a
    // lesson. A screen-scoped coroutine would die at the first navigation.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // A queue, not a single job. Buying the all-levels bundle grants five levels in one go, and
    // the previous "skip if something is already running" guard silently dropped four of them —
    // the parent paid for five and one arrived.
    private val queuedLevels = mutableListOf<String>()
    private val queueLock = Mutex()
    private var draining = false

    private val states = MutableStateFlow<Map<String, PackState>>(emptyMap())

    /** Live state per pack id, e.g. `"L1U3"`. Absent means "not looked at yet". */
    val packStates: StateFlow<Map<String, PackState>> = states.asStateFlow()

    /** Packs belonging to [levelId], in curriculum order — `["L1U3", "L1U4", …]`. */
    suspend fun packIdsForLevel(levelId: String): List<String> =
        manifestSource.load()
            .downloadablePackIds()
            .filter { it.startsWith("${levelId}U") }
            .sortedBy { it.substringAfter("U").toIntOrNull() ?: 0 }

    /** Bytes still missing across [levelId] — what a "download 8.9 MB?" prompt should show. */
    suspend fun pendingBytesForLevel(levelId: String): Long =
        packIdsForLevel(levelId).sumOf { downloader.pendingBytes(it) }

    /** Bytes currently stored on disk for [levelId], for the storage screen. */
    suspend fun storedBytesForLevel(levelId: String): Long {
        val manifest = manifestSource.load()
        return packIdsForLevel(levelId).sumOf { packId ->
            packFiles.sizeOf(manifest.assetsInPack(packId).values.map { it.hash })
        }
    }

    suspend fun refresh(packId: String): PackState {
        val manifest = manifestSource.load()
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
     * Makes sure [packId] is playable, downloading it if it is not, and reports whether it
     * ended up available. Progress lands in [packStates] so the unit's card animates while
     * this runs; a failure leaves the card showing retry.
     *
     * Callers use this before applying their own rules, so a pack whose earlier download
     * failed is retried by the very tap that needs it.
     */
    suspend fun ensureReady(packId: String): Boolean =
        when (refresh(packId)) {
            PackState.Bundled, PackState.Ready -> true
            // A failed download is not automatically a locked door. After an app update the
            // pack owes bytes for assets whose hash changed, yet every one of them still has
            // the previous copy on disk — offline, that unit plays exactly as it did
            // yesterday, so refusing entry would take away a lesson the child had finished.
            else -> runCatching { download(packId).collect { } }.isSuccess ||
                downloader.isPlayable(packId)
        }

    /**
     * Quietly fetches whatever [levelId] is still missing, one pack at a time, after the unit
     * the child actually tapped is already playing. Failures stay silent — this is a
     * head start, not something anyone is waiting on; a unit that misses out simply shows
     * its download badge when it is reached.
     */
    fun downloadLevelInBackground(levelId: String) {
        scope.launch {
            val startDraining = queueLock.withLock {
                if (levelId !in queuedLevels) queuedLevels += levelId
                if (draining) false else true.also { draining = true }
            }
            if (!startDraining) return@launch

            try {
                drainQueue()
            } finally {
                queueLock.withLock { draining = false }
            }
        }
    }

    /**
     * One level at a time, one pack at a time. Files inside a pack already run four abreast;
     * adding more parallelism across packs would just spread the same bandwidth thinner and
     * delay the unit the child is waiting on.
     */
    private suspend fun drainQueue() {
        while (true) {
            val levelId = queueLock.withLock { queuedLevels.removeFirstOrNull() } ?: return
            for (packId in packIdsForLevel(levelId)) {
                if (refresh(packId) !is PackState.NotDownloaded) continue
                runCatching { download(packId).collect { } }
            }
        }
    }

    /** Frees a pack's files. Access is untouched — the unit stays unlocked, just re-downloadable. */
    suspend fun delete(packId: String) {
        val manifest = manifestSource.load()
        packFiles.delete(manifest.assetsInPack(packId).values.map { it.hash })
        refresh(packId)
    }

    /** Drops every downloaded pack. QA tool: puts the device back to a just-installed state. */
    suspend fun deleteAll() {
        queueLock.withLock { queuedLevels.clear() }
        packFiles.clear()
        manifestSource.load().downloadablePackIds().forEach { refresh(it) }
    }

    /**
     * One-time-per-launch tidy after an app update. Cheap when there is nothing to do, so it
     * needs no "already done" flag: both steps are no-ops once the disk is clean.
     *
     * Two kinds of leftovers, from two different causes. Versions before content packs copied
     * bundled audio into an LRU cache that no longer has any owner in the code, and a content
     * update leaves the previous bytes of every changed asset behind under their old hash.
     * Neither has anything that would otherwise remove it.
     */
    suspend fun cleanUpAfterUpdate() {
        runCatching {
            packFiles.deleteLegacyAudioCache()
            sweepStaleFiles()
        }.onFailure { Napier.w("Post-update cleanup skipped: ${it.message}") }
    }

    /**
     * Sweeps files no longer referenced by the manifest — what a content update leaves
     * behind once an asset's bytes, and therefore its hash, changed.
     *
     * With one exception, and it is the whole point: an asset whose new bytes have not been
     * downloaded yet keeps its previous copy. Deleting on sight is what used to silence a
     * unit the child had already finished — the update lands, the old audio goes, and until
     * the device is next online the lesson has no sound. Holding the old file costs a little
     * disk until the new one arrives, and [AssetLocator] plays it in the meantime.
     */
    suspend fun sweepStaleFiles() {
        val manifest = manifestSource.load()
        val keep = hashesToKeep(
            currentHashes = manifest.assets.mapValues { it.value.hash },
            indexed = packIndex.snapshot(),
            isPresent = packFiles::has,
        )
        packFiles.deleteUnreferenced(keep)
        packIndex.pruneMissing()
    }

    private fun publish(packId: String, state: PackState) {
        states.value = states.value + (packId to state)
    }
}
