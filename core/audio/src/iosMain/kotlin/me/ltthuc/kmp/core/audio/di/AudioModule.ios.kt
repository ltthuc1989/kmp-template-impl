package me.ltthuc.kmp.core.audio.di

import me.ltthuc.kmp.core.audio.AudioCacheManager
import me.ltthuc.kmp.core.audio.AudioPlayer
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val audioPlatformModule: Module = module {
    single { AudioCacheManager() }
    single { AudioPlayer() }
    // Dedicated SFX-layer player so UI sounds don't cancel lesson playback.
    single(AudioPlayerSfx) { AudioPlayer() }
}
