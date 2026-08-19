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
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.PlayerEvent
import me.ltthuc.kmp.core.content.AssetLocator
import me.ltthuc.kmp.core.content.AssetSource
import me.ltthuc.kmp.core.content.ContentPackDownloader

/**
 * High-level audio facade for ViewModels. Single active playback — calling [play]
 * cancels any in-flight load and stops the current track. Resolves [AudioRef] →
 * Firebase URL → local cache file → native player.
 */
class AudioRepository(
    private val player: AudioPlayer,
    private val resolver: AudioAssetResolver,
    private val locator: AssetLocator,
    private val downloader: ContentPackDownloader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<AudioState>(AudioState.Idle)
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var currentRef: AudioRef? = null
    private var loadJob: Job? = null

    // Who started what is playing, so a screen that leaves cannot silence the screen that arrives.
    // Navigation disposes the outgoing screen only AFTER the incoming one has mounted and started
    // its audio (measured on a step→step jump: play(LetterSound(c)) at T, unconditional stop() from
    // the leaver at T+278ms), which swallowed the arriving screen's opening sound whole.
    private var currentOwner: Any? = null

    // Hàng chờ cho [playAll]. Level 2 step 0 là 4-6 file rời (guide vần + từng từ)
    // chứ không phải một file dài như Level 1, nên cần phát nối tiếp. Giữ ở đây
    // thay vì để ViewModel tự nghe trạng thái rồi gọi play() tiếp: chuyển bài phải
    // xảy ra ngay khi PlayerEvent.Completed tới, còn ViewModel thì có thể đã rời
    // màn hình. Một hàng chờ duy nhất, khớp với hợp đồng "một luồng phát" của lớp này.
    private var queue: List<AudioRef> = emptyList()
    private var queueIndex = 0

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
                        val next = queue.getOrNull(++queueIndex)
                        if (next != null) {
                            playInternal(next)
                            AudioState.Loading(next)
                        } else {
                            clearQueue()
                            currentRef = null
                            AudioState.Idle
                        }
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

    /**
     * [owner] claims this playback, so only that same owner can [stopFor] it. Leave it null and the
     * playback stays unowned — anyone may stop it, exactly as before.
     */
    fun play(ref: AudioRef, owner: Any? = null) {
        clearQueue()
        currentOwner = owner
        playInternal(ref)
    }

    /**
     * Phát [refs] nối tiếp nhau, hết bài này sang bài kia. Bỏ qua nếu rỗng.
     *
     * [state] trong lúc chạy luôn trỏ vào bài ĐANG phát, nên `isActiveFor(ref)` chỉ
     * đúng với đúng bài đó — màn hình muốn biết "cả chuỗi có đang chạy không" thì
     * kiểm tra ref hiện tại có nằm trong danh sách của mình hay không.
     */
    fun playAll(refs: List<AudioRef>, owner: Any? = null) {
        if (refs.isEmpty()) return
        currentOwner = owner
        queue = refs
        queueIndex = 0
        playInternal(refs.first())
    }

    private fun clearQueue() {
        queue = emptyList()
        queueIndex = 0
    }

    private fun playInternal(ref: AudioRef) {
        loadJob?.cancel()
        player.stop()
        currentRef = ref
        _state.value = AudioState.Loading(ref)

        loadJob = scope.launch {
            runCatching { ensurePlayable(ref) }
                .onSuccess { playable ->
                    // Guard against late completion racing a newer play() call.
                    if (currentRef != ref) return@onSuccess
                    when (playable) {
                        is Playable.Bundled -> player.playUri(playable.uri)
                        is Playable.LocalFile -> player.playFile(playable.path)
                    }
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
        clearQueue()
        loadJob?.cancel()
        player.stop()
        currentRef = null
        currentOwner = null
        _state.value = AudioState.Idle
    }

    /**
     * Stops only what [owner] itself started — what a screen wants when it is being disposed, since
     * by then the next screen may already be talking. Unowned playback (a [play] with no owner) is
     * still stopped, so callers that never claim ownership keep the old behaviour.
     */
    fun stopFor(owner: Any) {
        if (currentOwner == null || currentOwner === owner) stop()
    }

    suspend fun prefetch(refs: List<AudioRef>) {
        for (ref in refs) runCatching { ensurePlayable(ref) }
    }

    /**
     * Asks [AssetLocator] where the bytes are, and fetches them if they are not on the
     * device yet.
     *
     * Bundled audio is played straight out of the app, never copied. An earlier design
     * copied it into a path-keyed cache, and because that key never changed when the audio
     * behind it did, the copy shadowed the file shipped in the next build — an app update
     * could not deliver corrected audio (found 2026-08-14 with 7 stale Level 2 chants on
     * the test device). Downloaded files are keyed by content hash instead, so new bytes
     * simply live at a new name and the problem cannot recur.
     */
    private suspend fun ensurePlayable(ref: AudioRef): Playable {
        val logicalPath = resolver.logicalPath(ref)
        return when (val source = locator.resolve(logicalPath)) {
            is AssetSource.Bundled -> Playable.Bundled(source.uri)
            is AssetSource.Local -> Playable.LocalFile(source.path)
            is AssetSource.Remote -> Playable.LocalFile(downloader.fetchOne(logicalPath, source.asset))
            AssetSource.Missing -> error("No audio available for $ref ($logicalPath)")
        }
    }

    private sealed interface Playable {
        /** Asset inside the app — played in place, never copied to the cache. */
        data class Bundled(val uri: String) : Playable

        /** File on disk: downloaded audio held in the LRU cache. */
        data class LocalFile(val path: String) : Playable
    }
}
