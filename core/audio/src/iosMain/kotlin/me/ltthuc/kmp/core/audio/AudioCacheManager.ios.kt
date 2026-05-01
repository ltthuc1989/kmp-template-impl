package me.ltthuc.kmp.core.audio

import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileModificationDate
import platform.Foundation.NSFileSize
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class)
actual class AudioCacheManager(
    private val maxBytes: Long = DEFAULT_AUDIO_CACHE_MAX_BYTES,
) {
    private val cacheDirUrl: NSURL = run {
        val fm = NSFileManager.defaultManager
        val cachesUrl = fm.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("Unable to resolve iOS caches directory for AudioCacheManager")
        val dir = cachesUrl.URLByAppendingPathComponent(CACHE_SUBDIR)
            ?: error("Unable to compose audio cache subdir URL")
        fm.createDirectoryAtURL(dir, true, null, null)
        dir
    }

    actual fun cachedFilePath(cacheKey: String): String? {
        val fileUrl = cacheDirUrl.URLByAppendingPathComponent(cacheKey) ?: return null
        val path = fileUrl.path ?: return null
        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null
        // Touch to update LRU ordering on read.
        NSFileManager.defaultManager.setAttributes(
            mapOf<Any?, Any>(NSFileModificationDate to NSDate()),
            ofItemAtPath = path,
            error = null,
        )
        return path
    }

    @OptIn(BetaInteropApi::class)
    actual suspend fun put(cacheKey: String, bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val fileUrl = cacheDirUrl.URLByAppendingPathComponent(cacheKey)
            ?: error("Unable to compose audio cache file URL for '$cacheKey'")
        val data: NSData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        data.writeToURL(fileUrl, atomically = true)
        evictUntilUnderCap()
        fileUrl.path ?: error("Unable to read written audio cache file path")
    }

    actual fun evict(cacheKey: String) {
        val fileUrl = cacheDirUrl.URLByAppendingPathComponent(cacheKey) ?: return
        NSFileManager.defaultManager.removeItemAtURL(fileUrl, null)
    }

    actual fun clear() {
        val fm = NSFileManager.defaultManager
        val contents = fm.contentsOfDirectoryAtURL(cacheDirUrl, null, 0u, null)
            ?: return
        for (item in contents) {
            (item as? NSURL)?.let { fm.removeItemAtURL(it, null) }
        }
    }

    private fun evictUntilUnderCap() {
        val fm = NSFileManager.defaultManager
        val urls = fm.contentsOfDirectoryAtURL(cacheDirUrl, null, 0u, null)
            ?.mapNotNull { it as? NSURL }
            ?: return

        data class Entry(val url: NSURL, val size: Long, val mtime: Double)

        val entries = urls.mapNotNull { url ->
            val attrs = url.path?.let { fm.attributesOfItemAtPath(it, null) } ?: return@mapNotNull null
            val size = (attrs[NSFileSize] as? Number)?.toLong() ?: return@mapNotNull null
            val mtime = (attrs[NSFileModificationDate] as? NSDate)?.timeIntervalSince1970 ?: 0.0
            Entry(url, size, mtime)
        }.toMutableList()

        var total = entries.sumOf { it.size }
        if (total <= maxBytes) return

        entries.sortBy { it.mtime }
        val it = entries.iterator()
        while (total > maxBytes && it.hasNext()) {
            val victim = it.next()
            if (fm.removeItemAtURL(victim.url, null)) {
                total -= victim.size
                Napier.d("AudioCache evicted ${victim.url.lastPathComponent} (${victim.size} bytes)")
            }
        }
    }

    private companion object {
        const val CACHE_SUBDIR = "audio_cache"
    }
}
