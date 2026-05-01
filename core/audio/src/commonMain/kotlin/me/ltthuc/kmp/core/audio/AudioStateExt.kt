package me.ltthuc.kmp.core.audio

/** True when [AudioRepository] is loading or playing [ref]. */
fun AudioState.isActiveFor(ref: AudioRef): Boolean = when (this) {
    is AudioState.Loading -> this.ref == ref
    is AudioState.Playing -> this.ref == ref
    is AudioState.Paused -> false
    is AudioState.Error -> false
    AudioState.Idle -> false
}

/** Convenience: any non-idle state regardless of ref — use sparingly. */
fun AudioState.isActive(): Boolean = this !is AudioState.Idle && this !is AudioState.Error && this !is AudioState.Paused
