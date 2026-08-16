package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLIsExcludedFromBackupKey
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

@OptIn(ExperimentalForeignApi::class)
actual class PackStore : PackFiles {

    private val fileManager = NSFileManager.defaultManager

    private val dirUrl: NSURL = run {
        // Application Support, not Caches: iOS purges Caches under storage pressure and
        // paid curriculum must survive that.
        val support = fileManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("Unable to resolve iOS Application Support directory for PackStore")

        val dir = support.URLByAppendingPathComponent(STORE_DIR)
            ?: error("Unable to compose PackStore directory URL")
        fileManager.createDirectoryAtURL(dir, true, null, null)

        // Content is re-downloadable, so it must not eat the user's iCloud quota.
        dir.setResourceValue(NSNumber(bool = true), NSURLIsExcludedFromBackupKey, null)
        dir
    }

    private fun urlFor(hash: String): NSURL? = dirUrl.URLByAppendingPathComponent(hash)

    actual override fun pathFor(hash: String): String? {
        val path = urlFor(hash)?.path ?: return null
        return path.takeIf { fileManager.fileExistsAtPath(it) }
    }

    actual override fun has(hash: String): Boolean = pathFor(hash) != null

    @OptIn(BetaInteropApi::class)
    actual override suspend fun put(hash: String, bytes: ByteArray): String = withContext(Dispatchers.Default) {
        val target = urlFor(hash) ?: error("Unable to compose PackStore file URL for '$hash'")
        val data: NSData = bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
        // atomically = true writes to a temp file and renames, so a kill mid-write cannot
        // leave a truncated file that later looks complete.
        data.writeToURL(target, atomically = true)
        target.path ?: error("Unable to read written PackStore file path")
    }

    actual fun sizeOf(hashes: Collection<String>): Long = hashes.sumOf { hash ->
        val path = pathFor(hash) ?: return@sumOf 0L
        val attrs = fileManager.attributesOfItemAtPath(path, null)
        (attrs?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
    }

    actual fun delete(hashes: Collection<String>) {
        hashes.forEach { hash -> urlFor(hash)?.let { fileManager.removeItemAtURL(it, null) } }
    }

    actual fun deleteUnreferenced(keep: Set<String>) {
        val contents = fileManager.contentsOfDirectoryAtURL(dirUrl, null, 0u, null) ?: return
        for (item in contents) {
            val url = item as? NSURL ?: continue
            val name = url.lastPathComponent ?: continue
            if (name !in keep && fileManager.removeItemAtURL(url, null)) {
                Napier.d("PackStore swept $name")
            }
        }
    }

    actual fun clear() {
        val contents = fileManager.contentsOfDirectoryAtURL(dirUrl, null, 0u, null) ?: return
        for (item in contents) {
            (item as? NSURL)?.let { fileManager.removeItemAtURL(it, null) }
        }
    }

    private companion object {
        const val STORE_DIR = "content_packs"
    }
}
