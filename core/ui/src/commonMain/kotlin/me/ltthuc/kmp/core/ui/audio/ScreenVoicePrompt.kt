package me.ltthuc.kmp.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import org.koin.compose.koinInject

/**
 * Plays a spoken UI guidance prompt once when the screen appears (Khan Kids–style:
 * a friendly narrator tells the child what to do). Gated by the voice toggle / global
 * mute inside [SfxController] — silent when the parent turns voice off.
 *
 * Picks the `en` / `vi` clip from the effective UI locale ([LocalAppLocale]). [promptId]
 * matches the bundled file name under `files/sfx/prompts/<lang>/<promptId>.mp3`
 * (e.g. "vp_step_trace"). A small [delayMs] beat lets the screen settle before speaking.
 */
@Composable
fun ScreenVoicePrompt(promptId: String, delayMs: Long = 0L) {
    val sfx = koinInject<SfxController>()
    val lang = LocalAppLanguage.current
    LaunchedEffect(promptId) {
        if (delayMs > 0) delay(delayMs)
        sfx.playPrompt(promptId, lang)
    }
    // Stop this prompt the moment we leave the screen so audio never bleeds into the next
    // one (which may have no prompt). [stopPrompt] only stops if THIS prompt is still
    // playing, so it can't cut a newer screen's prompt regardless of dispose/compose order.
    DisposableEffect(promptId) {
        onDispose { sfx.stopPrompt(promptId) }
    }
}
