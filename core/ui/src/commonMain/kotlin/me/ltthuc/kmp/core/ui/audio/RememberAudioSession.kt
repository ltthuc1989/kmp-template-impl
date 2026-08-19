package me.ltthuc.kmp.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.AudioSession
import org.koin.compose.koinInject

/**
 * An [AudioSession] scoped to the calling composable: audio played through it is stopped when that
 * composable leaves, and only that audio — so leaving never silences the screen arriving behind it.
 *
 * For audio a screen starts itself (a spoken guide, a one-off congratulation). Audio driven from a
 * ViewModel belongs to a session the ViewModel holds, stopped from its own leave hook.
 */
@Composable
fun rememberAudioSession(): AudioSession {
    val repository = koinInject<AudioRepository>()
    val session = remember(repository) { AudioSession(repository) }
    DisposableEffect(session) {
        onDispose { session.stop() }
    }
    return session
}
