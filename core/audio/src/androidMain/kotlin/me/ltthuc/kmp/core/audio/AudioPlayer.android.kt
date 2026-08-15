package me.ltthuc.kmp.core.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class AudioPlayer(context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val _events = MutableStateFlow<PlayerEvent>(PlayerEvent.Idle)
    actual val events: StateFlow<PlayerEvent> = _events

    private val exo: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _events.value = PlayerEvent.Started(durationMs = duration.coerceAtLeast(0L))
                        startProgressLoop()
                    }
                    Player.STATE_ENDED -> {
                        stopProgressLoop()
                        _events.value = PlayerEvent.Completed
                    }
                    else -> Unit
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && playbackState != Player.STATE_ENDED) {
                    stopProgressLoop()
                    if (playbackState == Player.STATE_READY) {
                        _events.value = PlayerEvent.Paused
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Napier.e("ExoPlayer error", error)
                stopProgressLoop()
                _events.value = PlayerEvent.Failed(error)
            }
        })
    }

    private val progressTick = object : Runnable {
        override fun run() {
            if (exo.isPlaying) {
                _events.value = PlayerEvent.Progress(
                    positionMs = exo.currentPosition,
                    durationMs = exo.duration.coerceAtLeast(0L),
                )
                handler.postDelayed(this, PROGRESS_INTERVAL_MS)
            }
        }
    }

    actual fun playFile(absolutePath: String) = playUri("file://$absolutePath")

    /** ExoPlayer's `DefaultDataSource` already handles `file:///android_asset/...` URIs. */
    actual fun playUri(uri: String) {
        runOnMain {
            exo.stop()
            exo.clearMediaItems()
            exo.setMediaItem(MediaItem.fromUri(uri))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    actual fun pause() = runOnMain {
        if (exo.isPlaying) exo.pause()
    }

    actual fun resume() = runOnMain {
        if (!exo.isPlaying && exo.playbackState != Player.STATE_ENDED) {
            exo.play()
        }
    }

    actual fun stop() = runOnMain {
        stopProgressLoop()
        exo.stop()
        exo.clearMediaItems()
        _events.value = PlayerEvent.Idle
    }

    actual fun release() = runOnMain {
        stopProgressLoop()
        exo.release()
        _events.value = PlayerEvent.Idle
    }

    private fun startProgressLoop() {
        handler.removeCallbacks(progressTick)
        handler.post(progressTick)
    }

    private fun stopProgressLoop() {
        handler.removeCallbacks(progressTick)
    }

    private inline fun runOnMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post { block() }
    }

    private companion object {
        // 100ms, not 200ms: the L2 blend screen highlights cards whose sounds run 600-900ms, so a
        // 200ms sampling grid put the highlight up to a quarter of a phoneme late. Keep in sync
        // with PROGRESS_INTERVAL_SEC in the iOS actual.
        const val PROGRESS_INTERVAL_MS = 100L
    }
}
