package me.ltthuc.kmp.core.audio

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.isSuccess

/**
 * Downloads MP3 bytes from a public URL using Ktor. No retries here — caller decides
 * whether to retry or surface error to UX. Caller is also responsible for caching.
 */
class AudioDownloader(
    private val client: HttpClient,
) {
    suspend fun download(url: String): Result<ByteArray> = runCatching {
        Napier.d("AudioDownloader fetching $url")
        val response = client.get(url)
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value} fetching $url")
        }
        response.bodyAsBytes()
    }.onFailure { Napier.e("AudioDownloader failed for $url", it) }
}
