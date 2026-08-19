package me.ltthuc.kmp.core.content

import android.content.Context
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

actual class PackStore(context: Context) : PackFiles {

    // filesDir, not cacheDir: Android reclaims cacheDir under storage pressure and paid
    // curriculum must survive that.
    private val dir: File = File(context.filesDir, STORE_DIR).apply { mkdirs() }

    actual override fun pathFor(hash: String): String? = File(dir, hash).takeIf { it.isFile }?.absolutePath

    actual override fun has(hash: String): Boolean = File(dir, hash).isFile

    actual override suspend fun put(hash: String, bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val target = File(dir, hash)
        val tmp = File(dir, "$hash$TMP_SUFFIX")
        tmp.writeBytes(bytes)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            error("Unable to move downloaded asset into place: ${target.absolutePath}")
        }
        target.absolutePath
    }

    actual override fun sizeOf(hashes: Collection<String>): Long =
        hashes.sumOf { File(dir, it).takeIf { file -> file.isFile }?.length() ?: 0L }

    actual override fun delete(hashes: Collection<String>) {
        hashes.forEach { File(dir, it).delete() }
    }

    actual override fun deleteUnreferenced(keep: Set<String>) {
        dir.listFiles()?.forEach { file ->
            // The index is bookkeeping, not content — sweeping it would throw away the only
            // record of which lesson each stored hash belongs to.
            if (file.name == INDEX_FILE) return@forEach
            if (file.name.endsWith(TMP_SUFFIX) || file.name !in keep) {
                if (file.delete()) Napier.d("PackStore swept ${file.name}")
            }
        }
    }

    actual override fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    actual override fun deleteLegacyAudioCache() {
        val legacy = File(dir.parentFile, LEGACY_AUDIO_CACHE_DIR)
        if (legacy.isDirectory && legacy.deleteRecursively()) {
            Napier.i("Removed the pre-pack audio cache")
        }
    }

    actual override suspend fun readIndex(): String? = withContext(Dispatchers.IO) {
        File(dir, INDEX_FILE).takeIf { it.isFile }?.readText()
    }

    actual override suspend fun writeIndex(json: String): Unit = withContext(Dispatchers.IO) {
        val target = File(dir, INDEX_FILE)
        val tmp = File(dir, "$INDEX_FILE$TMP_SUFFIX")
        tmp.writeText(json)
        if (!tmp.renameTo(target)) {
            tmp.delete()
            error("Unable to move the pack index into place: ${target.absolutePath}")
        }
    }

    private companion object {
        const val STORE_DIR = "content_packs"
        const val LEGACY_AUDIO_CACHE_DIR = "audio_cache"
        const val TMP_SUFFIX = ".tmp"

        // Not hash-shaped, so it can never collide with a stored asset.
        const val INDEX_FILE = "index.json"
    }
}
