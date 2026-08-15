package me.ltthuc.kmp.core.audio.di

import io.ktor.client.HttpClient
import me.ltthuc.kmp.core.audio.AudioAssetResolver
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Koin qualifier for the dedicated SFX-layer [AudioPlayer] (separate from lesson voice). */
val AudioPlayerSfx = named("sfx")

/**
 * The single [HttpClient] lives here and is shared with `core:content`, so all content
 * traffic goes through one connection pool.
 */
val audioModule = module {
    single { HttpClient() }
    single { AudioAssetResolver() }

    includes(audioPlatformModule)
}

internal expect val audioPlatformModule: Module
