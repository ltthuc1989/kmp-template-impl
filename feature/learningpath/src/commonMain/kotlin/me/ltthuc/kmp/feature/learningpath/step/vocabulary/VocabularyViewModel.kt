package me.ltthuc.kmp.feature.learningpath.step.vocabulary

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
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef

internal class VocabularyViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    val screenState: StateFlow<ScreenState<VocabularyUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(VocabularyUiState(lessons = lessons.toImmutableList()))
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

    val audioState: StateFlow<AudioState> = audioRepository.state

    fun onListenWordToggle(lesson: PhonicsLesson, word: String) {
        val ref = lesson.wordRef(word) ?: run {
            Napier.w(tag = TAG) { "No Word audio ref for ${lesson.id}/$word" }
            return
        }
        when (val current = audioRepository.state.value) {
            is AudioState.Playing -> if (current.ref == ref) audioRepository.stop() else audioRepository.play(ref)
            is AudioState.Paused -> if (current.ref == ref) audioRepository.resume() else audioRepository.play(ref)
            is AudioState.Loading -> if (current.ref != ref) audioRepository.play(ref)
            else -> audioRepository.play(ref)
        }
    }

    fun onLeaveScreen() {
        audioRepository.stop()
    }

    private companion object {
        const val TAG = "VocabularyViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class VocabularyUiState(
    val lessons: ImmutableList<PhonicsLesson>,
)
