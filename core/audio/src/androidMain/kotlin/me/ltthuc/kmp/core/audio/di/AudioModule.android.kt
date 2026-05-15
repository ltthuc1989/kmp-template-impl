package me.ltthuc.kmp.core.audio.di

import android.content.Context
import me.ltthuc.kmp.core.audio.AudioCacheManager
import me.ltthuc.kmp.core.audio.AudioPlayer
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val audioPlatformModule: Module = module {
    single { AudioCacheManager(context = get<Context>()) }
    single { AudioPlayer(context = get<Context>()) }
    // Dedicated SFX-layer player so UI sounds don't cancel lesson playback.
    single(AudioPlayerSfx) { AudioPlayer(context = get<Context>()) }
}
