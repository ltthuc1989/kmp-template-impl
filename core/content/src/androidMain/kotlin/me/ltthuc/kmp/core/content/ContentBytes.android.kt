package me.ltthuc.kmp.core.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal actual suspend fun readFileBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
    File(path).readBytes()
}
