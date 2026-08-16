package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Where the manifest comes from. An interface so tests can supply one without Compose Resources. */
interface ManifestSource {
    suspend fun load(): ContentManifest
}

/**
 * Reads the bundled [ContentManifest] once per process and keeps it in memory.
 *
 * A missing or corrupt manifest degrades to [ContentManifest.EMPTY] rather than throwing:
 * with an empty manifest every asset resolves as bundled, which is exactly how the app
 * behaved before packs existed. A child mid-lesson must never hit a crash because a JSON
 * file failed to parse.
 */
class ContentManifestLoader : ManifestSource {
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var cached: ContentManifest? = null

    @OptIn(ExperimentalResourceApi::class)
    override suspend fun load(): ContentManifest {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: run {
                val loaded = runCatching {
                    json.decodeFromString<ContentManifest>(
                        Res.readBytes(ContentManifest.RESOURCE_PATH).decodeToString(),
                    )
                }.onFailure {
                    Napier.e("Content manifest unreadable — treating every asset as bundled", it)
                }.getOrDefault(ContentManifest.EMPTY)

                cached = loaded
                loaded
            }
        }
    }
}
