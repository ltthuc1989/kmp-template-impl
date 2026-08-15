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
import me.ltthuc.kmp.core.audio.AudioPlayer
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.content.AssetLocator
import me.ltthuc.kmp.core.content.AssetSource

/**
 * Khan Kids-style SFX layer: click chime, correct chime, voice praise, milestone fanfare.
 *
 * Owns its own [AudioPlayer] (injected with the `AudioPlayerSfx` qualifier) so that
 * firing a UI sound never cancels lesson playback in [AudioRepository]. Assets are
 * bundled-only — [AudioRef.Sfx], [AudioRef.Voice], [AudioRef.Music] all live in
 * composeResources under `files/sfx/` and are played straight out of the app in place;
 * nothing here is ever copied to disk.
 *
 * Settings contract (R2): [setVoiceEnabled] gates voice praise / nudge only, NEVER
 * the lesson phoneme audio (that flows through [AudioRepository]). [setGlobalMuted]
 * is the Khan-style master switch and overrides the three category toggles.
 */
class SfxController(
    private val player: AudioPlayer,
    private val resolver: AudioAssetResolver,
    private val locator: AssetLocator,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadJob: Job? = null

    // Id of the screen prompt currently playing (null = none). Lets [stopPrompt] avoid
    // cutting a newer screen's prompt when the previous screen disposes.
    private var currentPromptId: String? = null

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

    /**
     * Spoken UI guidance prompt ("Let's trace the letter!") played once on screen entry.
     * Gated by [voiceEnabled] like praise — never used for lesson curriculum audio.
     * [lang] is the effective UI locale ("en" / "vi").
     */
    fun playPrompt(promptId: String, lang: String) {
        if (globalMuted.value || !voiceEnabled.value) return
        play(AudioRef.Prompt(promptId, lang))
        currentPromptId = promptId
    }

    /**
     * Stops the screen prompt with [promptId] — but only if it is still the one playing.
     * Called when a screen leaves, so its prompt never bleeds into the next screen, while
     * a newer screen's prompt (which already replaced [currentPromptId]) is left untouched.
     */
    fun stopPrompt(promptId: String) {
        if (currentPromptId != promptId) return
        currentPromptId = null
        stop()
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

    /**
     * Resolves upcoming SFX ahead of time so the first tap-to-sound stays under ~50ms.
     * Nothing is copied to disk — this only pays the one-time "is it really bundled?"
     * probe, so the play path is a straight `Res.getUri` lookup.
     */
    suspend fun prefetch(names: List<String>) {
        for (n in names) runCatching { bundledUri(AudioRef.Sfx(n)) }
    }

    private fun play(ref: AudioRef) {
        currentPromptId = null
        loadJob?.cancel()
        player.stop()
        loadJob = scope.launch {
            runCatching { bundledUri(ref) }
                .onSuccess { uri -> player.playUri(uri) }
                .onFailure { Napier.e("SfxController play failed for $ref", it) }
        }
    }

    /**
     * SFX never comes off the network: a chime that arrives half a second late is worse
     * than no chime, and these assets are small enough to always ship in the app.
     */
    private suspend fun bundledUri(ref: AudioRef): String {
        val logicalPath = resolver.logicalPath(ref)
        return when (val source = locator.resolve(logicalPath)) {
            is AssetSource.Bundled -> source.uri
            else -> error("SFX asset not bundled: $logicalPath")
        }
    }
}
