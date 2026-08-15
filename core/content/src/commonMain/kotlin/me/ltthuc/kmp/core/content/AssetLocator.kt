package me.ltthuc.kmp.core.content

import io.github.aakira.napier.Napier
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/** Where the bytes for a logical asset path actually live right now. */
sealed interface AssetSource {
    /** Inside the app — play/read in place, never copy. */
    data class Bundled(val uri: String) : AssetSource

    /** Downloaded and pinned on disk. */
    data class Local(val path: String) : AssetSource

    /** Not on the device yet; [url] is immutable and safe to cache forever. */
    data class Remote(val url: String, val asset: ContentAsset) : AssetSource

    /** Neither shipped nor listed in the manifest — a content bug, not a runtime state. */
    data object Missing : AssetSource
}

/**
 * The one place that answers "where do I get this asset?".
 *
 * The manifest decides first: it is generated from the very tree that becomes the APK, so
 * "listed as downloadable" and "shipped in the app" cannot disagree. Asking it first also
 * avoids a failed `Res.readBytes` probe on every downloadable asset.
 *
 * Remote URLs embed the content hash (`content/<hash>/<logical path>`), so a file's URL
 * changes whenever its bytes change. That is what makes the on-disk copy safe to keep
 * forever with no revalidation, no ETag round-trip, and correct behaviour offline.
 */
class AssetLocator(
    private val manifestLoader: ContentManifestLoader,
    private val packStore: PackStore,
    private val cdnBaseUrl: String,
) {
    private val presence = mutableMapOf<String, Boolean>()
    private val presenceLock = Mutex()

    suspend fun resolve(logicalPath: String): AssetSource {
        val asset = manifestLoader.load().assets[logicalPath]
            ?: return bundledUri(logicalPath)
                ?.let { AssetSource.Bundled(it) }
                ?: AssetSource.Missing.also {
                    Napier.w("Asset '$logicalPath' is not in the app and not in the manifest")
                }

        packStore.pathFor(asset.hash)?.let { return AssetSource.Local(it) }
        return AssetSource.Remote(urlFor(logicalPath, asset), asset)
    }

    fun urlFor(logicalPath: String, asset: ContentAsset): String =
        contentUrl(cdnBaseUrl, logicalPath, asset)

    /**
     * URI of the in-app copy, or null when this path does not ship in the app.
     *
     * The manifest says which assets are downloadable, but not every remaining path is
     * really present — a lesson whose audio has not been generated yet resolves to a path
     * that simply is not there. `Res.getUri` cannot report that, so existence is probed
     * once per path with `Res.readBytes` and the verdict remembered. The probe reads but
     * never writes, so it can never leave a stale copy behind.
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun bundledUri(logicalPath: String): String? {
        val resourcePath = "files/$logicalPath"

        presenceLock.withLock { presence[resourcePath] }?.let { known ->
            return if (known) Res.getUri(resourcePath) else null
        }

        val present = runCatching { Res.readBytes(resourcePath) }
            .onFailure { Napier.d("Bundled asset miss for $resourcePath: ${it.message}") }
            .isSuccess
        presenceLock.withLock { presence[resourcePath] = present }

        return if (present) Res.getUri(resourcePath) else null
    }
}

/**
 * Immutable CDN URL for an asset: `<base>/<content hash>/<logical path>`.
 *
 * The hash sits in the path rather than in a query string so that every layer in between —
 * CDN edge, OS cache, our own [PackStore] — treats a changed file as a different file. That
 * is the whole reason no revalidation logic exists anywhere in this module.
 */
internal fun contentUrl(baseUrl: String, logicalPath: String, asset: ContentAsset): String =
    "${baseUrl.trimEnd('/')}/${asset.hash}/$logicalPath"
