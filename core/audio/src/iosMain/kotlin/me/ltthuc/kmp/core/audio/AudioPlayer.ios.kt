package me.ltthuc.kmp.core.audio

import io.github.aakira.napier.Napier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSError
import platform.Foundation.NSTimer
import platform.Foundation.NSURL
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayer {

    private val _events = MutableStateFlow<PlayerEvent>(PlayerEvent.Idle)
    actual val events: StateFlow<PlayerEvent> = _events

    private var current: AVAudioPlayer? = null
    private var delegate: AVAudioPlayerDelegateProtocol? = null
    private var progressTimer: NSTimer? = null

    init {
        // Activate playback session so audio plays through speakers (silent mode aware).
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
        }.onFailure { Napier.e("AVAudioSession activate failed", it) }
    }

    actual fun playFile(absolutePath: String) {
        stopInternal()
        val url = NSURL.fileURLWithPath(absolutePath)
        val player = AVAudioPlayer(contentsOfURL = url, error = null)
        if (player.prepareToPlay() && player.play()) {
            current = player
            val durationMs = (player.duration * 1000.0).toLong()
            val playerDelegate = PlaybackDelegate { didFinish ->
                stopProgressTimer()
                _events.value = if (didFinish) PlayerEvent.Completed else PlayerEvent.Idle
            }
            delegate = playerDelegate
            player.setDelegate(playerDelegate)
            _events.value = PlayerEvent.Started(durationMs = durationMs)
            startProgressTimer()
        } else {
            _events.value = PlayerEvent.Failed(IllegalStateException("AVAudioPlayer failed to start"))
        }
    }

    actual fun pause() {
        current?.let {
            if (it.playing) {
                it.pause()
                stopProgressTimer()
                _events.value = PlayerEvent.Paused
            }
        }
    }

    actual fun resume() {
        current?.let {
            if (!it.playing && it.play()) startProgressTimer()
        }
    }

    actual fun stop() {
        stopInternal()
        _events.value = PlayerEvent.Idle
    }

    actual fun release() {
        stopInternal()
        runCatching { AVAudioSession.sharedInstance().setActive(false, null) }
        _events.value = PlayerEvent.Idle
    }

    private fun stopInternal() {
        stopProgressTimer()
        current?.stop()
        current?.setDelegate(null)
        current = null
        delegate = null
    }

    private fun startProgressTimer() {
        stopProgressTimer()
        progressTimer = NSTimer.scheduledTimerWithTimeInterval(
            interval = PROGRESS_INTERVAL_SEC,
            repeats = true,
        ) {
            val player = current ?: return@scheduledTimerWithTimeInterval
            if (!player.playing) return@scheduledTimerWithTimeInterval
            _events.value = PlayerEvent.Progress(
                positionMs = (player.currentTime * 1000.0).toLong(),
                durationMs = (player.duration * 1000.0).toLong(),
            )
        }
    }

    private fun stopProgressTimer() {
        progressTimer?.invalidate()
        progressTimer = null
    }

    private companion object {
        const val PROGRESS_INTERVAL_SEC = 0.2
    }
}

@OptIn(ExperimentalForeignApi::class)
private class PlaybackDelegate(
    private val onFinish: (didFinish: Boolean) -> Unit,
) : NSObject(), AVAudioPlayerDelegateProtocol {

    override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
        onFinish(successfully)
    }

    override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
        Napier.e("AVAudioPlayer decode error: ${error?.localizedDescription}")
        onFinish(false)
    }
}
