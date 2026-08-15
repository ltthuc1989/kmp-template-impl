package me.ltthuc.kmp.core.content.di

import me.ltthuc.kmp.core.content.AssetLocator
import me.ltthuc.kmp.core.content.ContentManifestLoader
import me.ltthuc.kmp.core.content.ContentPackDownloader
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
    single { ContentManifestLoader() }
    single { AssetLocator(get(), get(), get<ContentConfig>().cdnBaseUrl) }
    single { ContentPackDownloader(get(), get(), get(), get()) }

    includes(contentPlatformModule)
}

internal expect val contentPlatformModule: Module
