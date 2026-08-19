package me.ltthuc.kmp.core.ui.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.SfxController
import org.koin.compose.koinInject

/**
 * The app's back stack, wrapped so that **navigating silences the screen being left** — at the
 * moment of the tap, not when that screen is finally disposed.
 *
 * Screens already stop their own audio in `onDispose`, but navigation runs its transition first
 * and only disposes the outgoing entry a few hundred ms later, once the next screen is on top.
 * So a chant / story / spoken guide kept talking over the screen the child had already moved to.
 * A back-stack mutation is the earliest honest signal that a screen is over, and it happens
 * before the arriving screen composes, so nothing here can cut the audio that screen starts.
 *
 * Hooking the mutation rather than each call site is deliberate: every push and pop in the app
 * goes through `LocalNavBackStack`, so a new navigation added later is covered without anyone
 * remembering to stop audio — the failure this replaces was exactly that kind of omission.
 *
 * Reads delegate straight through to the real (snapshot-backed) list, so recomposition on
 * navigation behaves as before. Give [NavDisplay][androidx.navigation3.ui.NavDisplay] the
 * undecorated list and route its `onBack` through this one, so system back silences too.
 */
// MutableParams: the back stack is mutable by nature and snapshot-backed, so Compose does observe
// it — that is the type `LocalNavBackStack` already hands every screen.
@Suppress("MutableParams")
@Composable
fun rememberSilencingNavBackStack(backStack: MutableList<NavKey>): MutableList<NavKey> {
    val audio = koinInject<AudioRepository>()
    val sfx = koinInject<SfxController>()
    return remember(backStack, audio, sfx) {
        SilencingNavBackStack(backStack) {
            // Lesson audio belongs to the screen that started it — always cut on leave. SFX only
            // gives up its narration: a chime fired by the navigating tap still rings out.
            audio.stop()
            sfx.stopSpeech()
        }
    }
}

private class SilencingNavBackStack(
    private val delegate: MutableList<NavKey>,
    private val silence: () -> Unit,
) : MutableList<NavKey> by delegate {

    override fun add(element: NavKey): Boolean {
        silence()
        return delegate.add(element)
    }

    override fun add(index: Int, element: NavKey) {
        silence()
        delegate.add(index, element)
    }

    override fun addAll(elements: Collection<NavKey>): Boolean {
        silence()
        return delegate.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<NavKey>): Boolean {
        silence()
        return delegate.addAll(index, elements)
    }

    override fun set(index: Int, element: NavKey): NavKey {
        silence()
        return delegate.set(index, element)
    }

    override fun remove(element: NavKey): Boolean {
        silence()
        return delegate.remove(element)
    }

    override fun removeAt(index: Int): NavKey {
        silence()
        return delegate.removeAt(index)
    }

    override fun removeAll(elements: Collection<NavKey>): Boolean {
        silence()
        return delegate.removeAll(elements)
    }

    override fun retainAll(elements: Collection<NavKey>): Boolean {
        silence()
        return delegate.retainAll(elements)
    }

    override fun clear() {
        silence()
        delegate.clear()
    }
}
