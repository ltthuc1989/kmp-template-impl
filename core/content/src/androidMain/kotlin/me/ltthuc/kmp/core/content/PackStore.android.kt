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

    actual fun sizeOf(hashes: Collection<String>): Long =
        hashes.sumOf { File(dir, it).takeIf { file -> file.isFile }?.length() ?: 0L }

    actual fun delete(hashes: Collection<String>) {
        hashes.forEach { File(dir, it).delete() }
    }

    actual fun deleteUnreferenced(keep: Set<String>) {
        dir.listFiles()?.forEach { file ->
            if (file.name.endsWith(TMP_SUFFIX) || file.name !in keep) {
                if (file.delete()) Napier.d("PackStore swept ${file.name}")
            }
        }
    }

    actual fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private companion object {
        const val STORE_DIR = "content_packs"
        const val TMP_SUFFIX = ".tmp"
    }
}
