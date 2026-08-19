package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** How far along a pack download is. [bytesTotal] counts only what still had to be fetched. */
data class DownloadProgress(
    val filesDone: Int,
    val filesTotal: Int,
    val bytesDone: Long,
    val bytesTotal: Long,
) {
    val isComplete: Boolean get() = filesDone >= filesTotal

    val fraction: Float get() = if (bytesTotal <= 0L) 1f else (bytesDone.toFloat() / bytesTotal)

    companion object {
        val COMPLETE = DownloadProgress(filesDone = 0, filesTotal = 0, bytesDone = 0L, bytesTotal = 0L)
    }
}

/**
 * Downloads one content pack into [PackStore], file by file.
 *
 * Resumable for free: files already in the store are skipped, so a download killed halfway
 * picks up where it stopped instead of starting over. Nothing needs to be recorded to make
 * that work — the hash-named files on disk *are* the record.
 *
 * Failures are per-file and fatal to the pack: one file that will not download after
 * [MAX_ATTEMPTS] fails the whole flow, because a pack missing one clip is a lesson that
 * breaks mid-way, which is worse than a download the user can retry.
 */
class ContentPackDownloader(
    private val client: HttpClient,
    private val manifestSource: ManifestSource,
    private val locator: AssetLocator,
    private val packFiles: PackFiles,
    private val packIndex: PackIndex,
) {
    /**
     * Emits progress until the pack is complete. Cancelling the collector cancels the
     * download; whatever finished stays on disk and counts on the next attempt.
     */
    fun download(packId: String): Flow<DownloadProgress> = channelFlow {
        val assets = manifestSource.load().assetsInPack(packId)
        val pending = assets.filterValues { !packFiles.has(it.hash) }

        if (pending.isEmpty()) {
            send(DownloadProgress.COMPLETE.copy(filesTotal = assets.size, filesDone = assets.size))
            return@channelFlow
        }

        val filesTotal = pending.size
        val bytesTotal = pending.values.sumOf { it.bytes }
        val lock = Mutex()
        var filesDone = 0
        var bytesDone = 0L

        Napier.i("Pack $packId: fetching $filesTotal file(s), $bytesTotal byte(s)")
        send(DownloadProgress(filesDone = 0, filesTotal = filesTotal, bytesDone = 0L, bytesTotal = bytesTotal))

        val gate = Semaphore(MAX_PARALLEL)
        val fetched = mutableMapOf<String, String>()
        try {
            coroutineScope {
                pending.map { (logicalPath, asset) ->
                    async {
                        gate.withPermit { fetch(logicalPath, asset) }
                        val progress = lock.withLock {
                            fetched[logicalPath] = asset.hash
                            filesDone += 1
                            bytesDone += asset.bytes
                            DownloadProgress(filesDone, filesTotal, bytesDone, bytesTotal)
                        }
                        send(progress)
                    }
                }.awaitAll()
            }
        } finally {
            // Recorded once per pack rather than per file: 50 index rewrites per pack would
            // cost far more than the one this replaces. NonCancellable so a user leaving the
            // screen still keeps the record of what did land.
            withContext(NonCancellable) { packIndex.record(fetched) }
        }
    }

    /**
     * Fetches one asset now and returns its on-disk path.
     *
     * The safety net for content reached before its pack finished — a deep link, a pack
     * download that failed halfway, a level unlocked while offline and opened later. It
     * lands in the same store as the pack, so the pack download afterwards skips it.
     */
    suspend fun fetchOne(logicalPath: String, asset: ContentAsset): String {
        fetch(logicalPath, asset)
        packIndex.record(mapOf(logicalPath to asset.hash))
        return packFiles.pathFor(asset.hash)
            ?: error("Downloaded $logicalPath but it is not in the pack store")
    }

    /** Bytes a pack still needs, for a "download will use X MB" prompt. */
    suspend fun pendingBytes(packId: String): Long =
        manifestSource.load().assetsInPack(packId)
            .values
            .filterNot { packFiles.has(it.hash) }
            .sumOf { it.bytes }

    /**
     * Whether every asset in the pack can be played from disk right now — the current bytes,
     * or the predecessor the sweep kept while an update is outstanding.
     *
     * Deliberately not the same question as [pendingBytes], which answers "how much is there
     * left to fetch". A pack can owe bytes and still be perfectly playable, and conflating
     * the two is what would keep a child out of a unit they had already finished just because
     * the device is offline when a newer take exists on the CDN.
     */
    suspend fun isPlayable(packId: String): Boolean {
        val assets = manifestSource.load().assetsInPack(packId)
        if (assets.isEmpty()) return true
        val previous = packIndex.snapshot()
        return assets.all { (logicalPath, asset) ->
            packFiles.has(asset.hash) || previous[logicalPath]?.let(packFiles::has) == true
        }
    }

    private suspend fun fetch(logicalPath: String, asset: ContentAsset) {
        val url = locator.urlFor(logicalPath, asset)
        var lastError: Throwable? = null

        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching {
                val response = client.get(url)
                if (!response.status.isSuccess()) {
                    error("HTTP ${response.status.value} for $url")
                }
                response.bodyAsBytes()
            }

            result.onSuccess { bytes ->
                packFiles.put(asset.hash, bytes)
                return
            }.onFailure { cause ->
                lastError = cause
                Napier.w("Pack fetch attempt ${attempt + 1} failed for $logicalPath: ${cause.message}")
                // Back off before trying again. Three retries fired back-to-back all hit the same
                // half-second of bad signal and fail together, which is a slower way of not
                // retrying at all. Test time is virtual, so this costs the suite nothing.
                if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_BACKOFF_MS * (attempt + 1))
            }
        }

        throw IllegalStateException("Unable to download $logicalPath after $MAX_ATTEMPTS attempts", lastError)
    }

    private companion object {
        const val MAX_PARALLEL = 4
        const val MAX_ATTEMPTS = 3
        const val RETRY_BACKOFF_MS = 400L
    }
}
