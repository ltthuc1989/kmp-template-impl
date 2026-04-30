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
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.resource.error_no_data
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class StoryViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
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

    private companion object {
        const val TAG = "StoryViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
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
