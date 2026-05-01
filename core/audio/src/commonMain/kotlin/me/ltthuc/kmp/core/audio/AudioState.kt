package me.ltthuc.kmp.core.audio

sealed interface AudioState {
    data object Idle : AudioState
    data class Loading(val ref: AudioRef) : AudioState
    data class Playing(val ref: AudioRef, val positionMs: Long, val durationMs: Long) : AudioState
    data class Paused(val ref: AudioRef, val positionMs: Long) : AudioState
    data class Error(val ref: AudioRef, val cause: Throwable) : AudioState
}
