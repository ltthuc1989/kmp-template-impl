package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor

/**
 * Play [ref], then suspend until playback finishes (reaches [AudioState.Idle]) or errors,
 * capped at [timeoutMs] so a missing/broken asset can never hang the caller.
 *
 * Two-stage wait: first until the ref is actively loading/playing, then until it ends. Use when
 * UI must be sequenced on audio completion (e.g. play a guide prompt, then reveal game content).
 */
suspend fun AudioRepository.playAndAwait(ref: AudioRef, timeoutMs: Long = 6_000L) {
    play(ref)
    withTimeoutOrNull(timeoutMs) {
        state.first { it.isActiveFor(ref) }
        state.first { it is AudioState.Idle || it is AudioState.Error }
    }
}
