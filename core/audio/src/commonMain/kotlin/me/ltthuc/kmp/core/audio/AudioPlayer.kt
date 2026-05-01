package me.ltthuc.kmp.core.audio

import kotlinx.coroutines.flow.StateFlow

/**
 * Low-level platform audio player. Loads a single local file path and emits playback
 * lifecycle events. Higher-level resolution (URL → cache → file) lives in AudioRepository.
 *
 * Single-track only — call [stop] before [load] of a new file. Implementations are not
 * required to be thread-safe; callers should drive from a single coroutine.
 */
expect class AudioPlayer {

    /** Emits [PlayerEvent] for the active load. */
    val events: StateFlow<PlayerEvent>

    /** Loads the file at [absolutePath] and starts playback. Cancels any previous track. */
    fun playFile(absolutePath: String)

    fun pause()

    fun resume()

    fun stop()

    /** Releases native resources. After release, the instance is unusable. */
    fun release()
}

sealed interface PlayerEvent {
    data object Idle : PlayerEvent
    data class Started(val durationMs: Long) : PlayerEvent
    data class Progress(val positionMs: Long, val durationMs: Long) : PlayerEvent
    data object Paused : PlayerEvent
    data object Completed : PlayerEvent
    data class Failed(val cause: Throwable) : PlayerEvent
}
