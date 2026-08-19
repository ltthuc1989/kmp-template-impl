package me.ltthuc.kmp.core.content.di

import me.ltthuc.kmp.core.content.AssetLocator
import me.ltthuc.kmp.core.content.ContentBytes
import me.ltthuc.kmp.core.content.ContentManifestLoader
import me.ltthuc.kmp.core.content.ContentPackDownloader
import me.ltthuc.kmp.core.content.ManifestSource
import me.ltthuc.kmp.core.content.PackFiles
import me.ltthuc.kmp.core.content.PackIndex
import me.ltthuc.kmp.core.content.PackStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Where downloadable content lives, provided by the host app so staging and production can
 * differ without recompiling. Paths under it are `<hash>/<logical path>` and immutable, so
 * the CDN may cache them forever.
 */
data class ContentConfig(
    val cdnBaseUrl: String,
)

/**
 * Requires an `HttpClient` in the graph — `audioModule` already registers one, and sharing
 * it keeps a single connection pool for all content traffic.
 */
val contentModule = module {
    // Bound by interface, not by concrete type: the download path depends on ManifestSource and
    // PackFiles so it can be tested without Compose Resources or a real filesystem, and Koin
    // resolves by the declared type. Registering only the concrete classes compiles fine and
    // then fails at startup.
    single<ManifestSource> { ContentManifestLoader() }
    single<PackFiles> { get<PackStore>() }
    single { PackIndex(get()) }
    single { AssetLocator(get(), get(), get(), get<ContentConfig>().cdnBaseUrl) }
    single { ContentPackDownloader(get(), get(), get(), get(), get()) }
    single { ContentBytes(get(), get()) }

    includes(contentPlatformModule)
}

internal expect val contentPlatformModule: Module
