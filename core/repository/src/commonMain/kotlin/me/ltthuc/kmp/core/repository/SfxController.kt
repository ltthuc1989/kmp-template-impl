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
import me.ltthuc.kmp.core.audio.AudioPlayer
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.resource.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Khan Kids-style SFX layer: click chime, correct chime, voice praise, milestone fanfare.
 *
 * Owns its own [AudioPlayer] (injected with the `AudioPlayerSfx` qualifier) so that
 * firing a UI sound never cancels lesson playback in [AudioRepository]. Assets are
 * bundled-only — [AudioRef.Sfx], [AudioRef.Voice], [AudioRef.Music] all live in
 * composeResources under `files/sfx/`.
 *
 * Settings contract (R2): [setVoiceEnabled] gates voice praise / nudge only, NEVER
 * the lesson phoneme audio (that flows through [AudioRepository]). [setGlobalMuted]
 * is the Khan-style master switch and overrides the three category toggles.
 */
class SfxController(
    private val player: AudioPlayer,
    private val resolver: AudioAssetResolver,
    private val cache: AudioCacheManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadJob: Job? = null

    private val sfxEnabled = MutableStateFlow(true)
    private val voiceEnabled = MutableStateFlow(true)
    private val musicEnabled = MutableStateFlow(true)
    private val globalMuted = MutableStateFlow(false)

    val isMuted: StateFlow<Boolean> = globalMuted.asStateFlow()

    fun configure(
        sfxEnabled: Boolean,
        voiceEnabled: Boolean,
        musicEnabled: Boolean,
        globalMuted: Boolean,
    ) {
        this.sfxEnabled.value = sfxEnabled
        this.voiceEnabled.value = voiceEnabled
        this.musicEnabled.value = musicEnabled
        this.globalMuted.value = globalMuted
    }

    /** UI chime / click / lesson_complete. Khan-simple: fires every time the caller asks. */
    fun playSfx(name: String) {
        if (globalMuted.value || !sfxEnabled.value) return
        play(AudioRef.Sfx(name))
    }

    /**
     * Voice praise ("Great job!") or nudge ("Try again!"). Gated by [voiceEnabled].
     * Never call this for lesson phoneme audio — phoneme audio is curriculum and must
     * play even when the parent has turned voice praise off.
     */
    fun playVoicePraise(name: String) {
        if (globalMuted.value || !voiceEnabled.value) return
        play(AudioRef.Voice(name))
    }

    /** Background music loop. No-op until BGM assets ship (phase 1.1). */
    fun playMusic(name: String) {
        if (globalMuted.value || !musicEnabled.value) return
        play(AudioRef.Music(name))
    }

    fun stop() {
        loadJob?.cancel()
        player.stop()
    }

    /** Warms the cache for upcoming SFX so the first tap-to-sound stays under ~50ms. */
    suspend fun prefetch(names: List<String>) {
        for (n in names) runCatching { ensurePlayable(AudioRef.Sfx(n)) }
    }

    private fun play(ref: AudioRef) {
        loadJob?.cancel()
        player.stop()
        loadJob = scope.launch {
            runCatching { ensurePlayable(ref) }
                .onSuccess { path -> player.playFile(path) }
                .onFailure { Napier.e("SfxController play failed for $ref", it) }
        }
    }

    private suspend fun ensurePlayable(ref: AudioRef): String {
        val cacheKey = resolver.cacheKey(ref)
        cache.cachedFilePath(cacheKey)?.let { return it }
        val bytes = readBundled(ref) ?: error("SFX asset not bundled: $ref")
        return cache.put(cacheKey, bytes)
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBundled(ref: AudioRef): ByteArray? {
        val path = resolver.bundledResourcePath(ref) ?: return null
        return runCatching { Res.readBytes(path) }
            .onFailure { Napier.d("Bundled SFX miss for $path: ${it.message}") }
            .getOrNull()
    }
}
