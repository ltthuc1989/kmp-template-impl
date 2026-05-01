package me.ltthuc.kmp.feature.learningpath.step.story

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
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class StoryViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    private val audioRepository: AudioRepository,
) : ViewModel() {

    val screenState: StateFlow<ScreenState<StoryUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    val pages = lessons
                        .sortedBy { it.orderIndex }
                        .flatMap { lesson ->
                            lesson.words.map { word ->
                                StoryPage(letter = lesson.displayLetter.firstOrNull() ?: '?', word = word)
                            }
                        }
                    ScreenState.Idle(
                        StoryUiState(
                            lessons = lessons.toImmutableList(),
                            pages = pages.toImmutableList(),
                        ),
                    )
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

    val storyRef: AudioRef.Story? = unitId.toStoryId()?.let(AudioRef::Story)

    fun onListenToggle() {
        val ref = storyRef ?: run {
            Napier.w(tag = TAG) { "No Story audio ref for unit $unitId" }
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

    /** "L1U1" → "L1_S01" (story id matches stories.json). */
    private fun String.toStoryId(): String? {
        val match = UNIT_ID_REGEX.matchEntire(this) ?: return null
        val (level, unit) = match.destructured
        return "L${level}_S${unit.padStart(2, '0')}"
    }

    private companion object {
        const val TAG = "StoryViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        val UNIT_ID_REGEX = Regex("""L(\d+)U(\d+)""")
    }
}

@Immutable
internal data class StoryUiState(
    val lessons: ImmutableList<PhonicsLesson>,
    val pages: ImmutableList<StoryPage>,
)

@Immutable
internal data class StoryPage(
    val letter: Char,
    val word: LessonWord,
)
