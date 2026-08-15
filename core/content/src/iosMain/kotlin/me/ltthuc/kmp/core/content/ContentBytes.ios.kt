package me.ltthuc.kmp.core.content

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun readFileBytes(path: String): ByteArray = withContext(Dispatchers.Default) {
    val data: NSData = NSData.dataWithContentsOfFile(path)
        ?: error("Unable to read content file at $path")

    ByteArray(data.length.toInt()).apply {
        if (isNotEmpty()) {
            usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }
    }
}
