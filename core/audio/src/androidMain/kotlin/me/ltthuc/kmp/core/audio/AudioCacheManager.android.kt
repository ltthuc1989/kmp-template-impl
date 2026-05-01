package me.ltthuc.kmp.core.audio

import android.content.Context
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class AudioCacheManager(
    context: Context,
    private val maxBytes: Long = DEFAULT_AUDIO_CACHE_MAX_BYTES,
) {
    private val cacheDir: File = File(context.filesDir, CACHE_SUBDIR).apply { mkdirs() }

    actual fun cachedFilePath(cacheKey: String): String? {
        val file = File(cacheDir, cacheKey)
        if (!file.isFile) return null
        // Touch to update LRU ordering on read.
        file.setLastModified(System.currentTimeMillis())
        return file.absolutePath
    }

    actual suspend fun put(cacheKey: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val file = File(cacheDir, cacheKey)
        file.writeBytes(bytes)
        evictUntilUnderCap()
        file.absolutePath
    }

    actual fun evict(cacheKey: String) {
        File(cacheDir, cacheKey).delete()
    }

    actual fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun evictUntilUnderCap() {
        val files = cacheDir.listFiles()?.toMutableList() ?: return
        var total = files.sumOf { it.length() }
        if (total <= maxBytes) return

        files.sortBy { it.lastModified() }
        val it = files.iterator()
        while (total > maxBytes && it.hasNext()) {
            val victim = it.next()
            val size = victim.length()
            if (victim.delete()) {
                total -= size
                Napier.d("AudioCache evicted ${victim.name} ($size bytes)")
            }
        }
    }

    private companion object {
        const val CACHE_SUBDIR = "audio_cache"
    }
}
