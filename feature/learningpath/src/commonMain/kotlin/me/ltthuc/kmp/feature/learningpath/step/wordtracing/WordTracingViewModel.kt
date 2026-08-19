package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.AudioSession
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef

/**
 * Level 2+ Tracing step. One slide = the lesson's words; each word is traced letter-by-letter,
 * Duolingo-style. Loads the unit's lessons and voices the isolated letter sound as each letter
 * starts; the whole word (lesson vocab audio) is kept for the end, once the word is fully traced.
 * Playback is fire-and-forget so tracing never stalls on a missing asset.
 */
internal class WordTracingViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    // This screen's claim on the single playback channel: what it starts, only it can stop. Keeps the
    // outgoing screen's stop (which runs mid nav-transition) from cutting the incoming screen's audio.
    private val audio = AudioSession(audioRepository)

    val screenState: StateFlow<ScreenState<WordTracingUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(WordTracingUiState(lessons = lessons.toImmutableList()))
                }
            }
            .catch { throwable ->
                Napier.e(tag = TAG, throwable = throwable) { "Failed to load lessons for $unitId" }
                emit(ScreenState.Error(message = Res.string.error_network))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = ScreenState.Loading(),
            )

    /** Whole-word vocab audio, played on its own (e.g. over the picture when a word is finished). */
    fun playWord(lesson: PhonicsLesson, word: String) {
        val ref = lesson.wordRef(word) ?: run {
            Napier.w(tag = TAG) { "No Word audio ref for ${lesson.id}/$word" }
            return
        }
        audio.play(ref)
    }

    /** Isolated single-letter phoneme, e.g. /t/, played as each letter starts — the first included. */
    fun playLetter(letter: Char) {
        if (!letter.isLetter()) return
        audio.play(AudioRef.LetterSound(letter.toString()))
    }

    fun onLeaveScreen() {
        audio.stop()
    }

    private companion object {
        const val TAG = "WordTracingViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class WordTracingUiState(
    val lessons: ImmutableList<PhonicsLesson>,
)
