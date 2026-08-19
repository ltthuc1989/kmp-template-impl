package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.StateFlow
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState

/**
 * One screen's handle on the app's single playback channel.
 *
 * Holding the session IS the ownership claim: everything played through it is tagged with it, and
 * [stop] silences only what this session started. That is what lets a screen stop its own audio on
 * the way out without cutting off the screen that is arriving — navigation disposes the outgoing
 * screen a few hundred ms AFTER the incoming one has already started talking.
 *
 * Prefer this over calling [AudioRepository.play] with an `owner` by hand. Passing the owner at each
 * call site invites the bug it exists to prevent: inside `viewModelScope.launch { }` a bare `this` is
 * the CoroutineScope, not the ViewModel, so the claim silently does not match the later `stop`.
 *
 * One session per screen (a ViewModel field, or `rememberAudioSession()` for screen-local audio).
 */
class AudioSession(private val repository: AudioRepository) {

    val state: StateFlow<AudioState> get() = repository.state

    fun play(ref: AudioRef) = repository.play(ref, owner = this)

    /** Plays [refs] back to back — see [AudioRepository.playAll]. */
    fun playAll(refs: List<AudioRef>) = repository.playAll(refs, owner = this)

    /** Plays [ref] and suspends until it ends — see [playAndAwait]. */
    suspend fun playAndAwait(ref: AudioRef, timeoutMs: Long = AWAIT_TIMEOUT_MS) =
        repository.playAndAwait(ref, timeoutMs, owner = this)

    fun pause() = repository.pause()

    fun resume() = repository.resume()

    /** Stops this session's playback, and nothing else's. */
    fun stop() = repository.stopFor(this)

    private companion object {
        const val AWAIT_TIMEOUT_MS = 6_000L
    }
}
