package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

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
    private val manifestLoader: ContentManifestLoader,
    private val locator: AssetLocator,
    private val packStore: PackStore,
) {
    /**
     * Emits progress until the pack is complete. Cancelling the collector cancels the
     * download; whatever finished stays on disk and counts on the next attempt.
     */
    fun download(packId: String): Flow<DownloadProgress> = channelFlow {
        val assets = manifestLoader.load().assetsInPack(packId)
        val pending = assets.filterValues { !packStore.has(it.hash) }

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
        coroutineScope {
            pending.map { (logicalPath, asset) ->
                async {
                    gate.withPermit { fetch(logicalPath, asset) }
                    val progress = lock.withLock {
                        filesDone += 1
                        bytesDone += asset.bytes
                        DownloadProgress(filesDone, filesTotal, bytesDone, bytesTotal)
                    }
                    send(progress)
                }
            }.awaitAll()
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
        return packStore.pathFor(asset.hash)
            ?: error("Downloaded $logicalPath but it is not in the pack store")
    }

    /** Bytes a pack still needs, for a "download will use X MB" prompt. */
    suspend fun pendingBytes(packId: String): Long =
        manifestLoader.load().assetsInPack(packId)
            .values
            .filterNot { packStore.has(it.hash) }
            .sumOf { it.bytes }

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
                packStore.put(asset.hash, bytes)
                return
            }.onFailure { cause ->
                lastError = cause
                Napier.w("Pack fetch attempt ${attempt + 1} failed for $logicalPath: ${cause.message}")
            }
        }

        throw IllegalStateException("Unable to download $logicalPath after $MAX_ATTEMPTS attempts", lastError)
    }

    private companion object {
        const val MAX_PARALLEL = 4
        const val MAX_ATTEMPTS = 3
    }
}
