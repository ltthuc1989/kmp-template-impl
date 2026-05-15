package me.ltthuc.kmp.core.audio.di

import io.ktor.client.HttpClient
import me.ltthuc.kmp.core.audio.AudioAssetResolver
import me.ltthuc.kmp.core.audio.AudioDownloader
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Configuration provided by the host app (e.g. composeApp) — currently just the
 * Firebase Storage bucket. Kept as a Koin-injected POKO so callers can swap buckets
 * for staging/production without recompiling [AudioAssetResolver].
 */
data class AudioConfig(
    val storageBucket: String,
)

/** Koin qualifier for the dedicated SFX-layer [AudioPlayer] (separate from lesson voice). */
val AudioPlayerSfx = named("sfx")

val audioModule = module {
    single { HttpClient() }
    single { AudioDownloader(get()) }
    single { AudioAssetResolver(storageBucket = get<AudioConfig>().storageBucket) }

    includes(audioPlatformModule)
}

internal expect val audioPlatformModule: Module
