package me.ltthuc.kmp.core.content

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Every message in the cause chain. Coroutines re-wrap exceptions on the way out of
 * `coroutineScope`, so the interesting cause sits a level or two down — which is exactly why
 * production classifies failures by walking the chain too.
 */
fun Throwable.chainMessages(): String = generateSequence(this) { it.cause }
    .mapNotNull { it.message }
    .joinToString(" | ")

/** In-memory [PackFiles]; [failOnPut] simulates a device that has run out of space. */
class FakePackFiles(
    private val failOnPut: String? = null,
) : PackFiles {
    private val stored = mutableMapOf<String, ByteArray>()
    private val lock = Mutex()

    val storedHashes: Set<String> get() = stored.keys.toSet()

    /** True once the pre-pack audio cache has been asked to go. */
    var legacyCacheDeleted: Boolean = false
        private set

    override fun pathFor(hash: String): String? = if (stored.containsKey(hash)) "/packs/$hash" else null

    override fun has(hash: String): Boolean = stored.containsKey(hash)

    override suspend fun put(hash: String, bytes: ByteArray): String = lock.withLock {
        if (hash == failOnPut) error("write failed: No space left on device")
        stored[hash] = bytes
        "/packs/$hash"
    }

    override fun sizeOf(hashes: Collection<String>): Long =
        hashes.sumOf { stored[it]?.size?.toLong() ?: 0L }

    override fun delete(hashes: Collection<String>) {
        hashes.forEach { stored.remove(it) }
    }

    override fun deleteUnreferenced(keep: Set<String>) {
        stored.keys.retainAll(keep)
    }

    override fun clear() {
        stored.clear()
    }

    override fun deleteLegacyAudioCache() {
        legacyCacheDeleted = true
    }

    fun seed(hash: String) {
        stored[hash] = ByteArray(1)
    }
}

class FakeManifestSource(private val manifest: ContentManifest) : ManifestSource {
    override suspend fun load(): ContentManifest = manifest
}

fun manifestOf(
    vararg assets: Pair<String, ContentAsset>,
    packs: Map<String, PackInfo> = emptyMap(),
): ContentManifest = ContentManifest(
    version = 1,
    hashLength = 10,
    packs = packs,
    assets = assets.toMap(),
)

fun asset(hash: String, pack: String, bytes: Long = 100) = ContentAsset(hash = hash, bytes = bytes, pack = pack)

/** Serves everything except the URL carrying [deadHash], which always fails. */
class DeadUrlServer(private val deadHash: String) : FlakyServer() {
    override fun handle(url: String, attempt: Int): Boolean = !url.contains(deadHash)
}

/**
 * Serves 200s, but the first [failuresPerUrl] requests for any URL fail — enough to exercise
 * the retry ladder without depending on timing.
 */
open class FlakyServer(
    private val failuresPerUrl: Int = 0,
    private val status: HttpStatusCode = HttpStatusCode.InternalServerError,
) {
    private val attempts = mutableMapOf<String, Int>()

    val requestCount: Int get() = attempts.values.sum()

    /** True to serve the file, false to fail this attempt. */
    protected open fun handle(url: String, attempt: Int): Boolean = attempt > failuresPerUrl

    fun attemptsFor(urlPart: String): Int =
        attempts.entries.firstOrNull { it.key.contains(urlPart) }?.value ?: 0

    fun client(): HttpClient = HttpClient(
        MockEngine { request ->
            val url = request.url.toString()
            val seen = (attempts[url] ?: 0) + 1
            attempts[url] = seen
            if (!handle(url, seen)) {
                respondError(status)
            } else {
                respond(
                    content = ByteArray(8) { 7 },
                    status = HttpStatusCode.OK,
                    headers = headersOf("Content-Type", "audio/mpeg"),
                )
            }
        },
    )
}
