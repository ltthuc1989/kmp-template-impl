package me.ltthuc.kmp.core.repository

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioAssetResolver
import me.ltthuc.kmp.core.audio.AudioCacheManager
import me.ltthuc.kmp.core.audio.AudioDownloader
import me.ltthuc.kmp.core.audio.AudioPlayer
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.PlayerEvent
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * High-level audio facade for ViewModels. Single active playback — calling [play]
 * cancels any in-flight load and stops the current track. Resolves [AudioRef] →
 * Firebase URL → local cache file → native player.
 */
class AudioRepository(
    private val player: AudioPlayer,
    private val downloader: AudioDownloader,
    private val cache: AudioCacheManager,
    private val resolver: AudioAssetResolver,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AudioState>(AudioState.Idle)
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var currentRef: AudioRef? = null
    private var loadJob: Job? = null

    init {
        scope.launch {
            player.events.collect { event ->
                val ref = currentRef ?: return@collect
                _state.value = when (event) {
                    is PlayerEvent.Started -> AudioState.Playing(ref, positionMs = 0L, durationMs = event.durationMs)
                    is PlayerEvent.Progress -> AudioState.Playing(ref, event.positionMs, event.durationMs)
                    PlayerEvent.Paused -> {
                        val current = _state.value as? AudioState.Playing
                        AudioState.Paused(ref, positionMs = current?.positionMs ?: 0L)
                    }
                    PlayerEvent.Completed -> {
                        currentRef = null
                        AudioState.Idle
                    }
                    is PlayerEvent.Failed -> {
                        currentRef = null
                        AudioState.Error(ref, event.cause)
                    }
                    PlayerEvent.Idle -> _state.value
                }
            }
        }
    }

    fun play(ref: AudioRef) {
        loadJob?.cancel()
        player.stop()
        currentRef = ref
        _state.value = AudioState.Loading(ref)

        loadJob = scope.launch {
            runCatching { ensurePlayable(ref) }
                .onSuccess { path ->
                    // Guard against late completion racing a newer play() call.
                    if (currentRef == ref) player.playFile(path)
                }
                .onFailure { cause ->
                    Napier.e("AudioRepository load failed for $ref", cause)
                    if (currentRef == ref) _state.value = AudioState.Error(ref, cause)
                }
        }
    }

    fun pause() = player.pause()
    fun resume() = player.resume()

    fun stop() {
        loadJob?.cancel()
        player.stop()
        currentRef = null
        _state.value = AudioState.Idle
    }

    suspend fun prefetch(refs: List<AudioRef>) {
        for (ref in refs) runCatching { ensurePlayable(ref) }
    }

    private suspend fun ensurePlayable(ref: AudioRef): String {
        val cacheKey = resolver.cacheKey(ref)
        cache.cachedFilePath(cacheKey)?.let { return it }

        // Bundled assets (Compose Resources) win over network — supports offline dev
        // and ships pre-baked content without Firebase setup.
        readBundled(ref)?.let { bytes -> return cache.put(cacheKey, bytes) }

        val url = resolver.downloadUrl(ref)
        val bytes = downloader.download(url).getOrThrow()
        return cache.put(cacheKey, bytes)
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBundled(ref: AudioRef): ByteArray? {
        val path = resolver.bundledResourcePath(ref) ?: return null
        return runCatching { Res.readBytes(path) }
            .onFailure { Napier.d("Bundled audio miss for $path: ${it.message}") }
            .getOrNull()
    }
}
