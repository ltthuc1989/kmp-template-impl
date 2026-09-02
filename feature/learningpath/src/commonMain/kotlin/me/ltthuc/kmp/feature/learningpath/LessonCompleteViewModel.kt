package me.ltthuc.kmp.feature.learningpath

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
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
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class LessonCompleteViewModel(
    private val unitId: String,
    private val lessonIndex: Int,
    unitRepository: UnitRepository,
) : ViewModel() {

    val state: StateFlow<ScreenState<LessonCompleteUiState>> = unitRepository.observeLessons(unitId)
        .map<List<PhonicsLesson>, ScreenState<LessonCompleteUiState>> { lessons ->
            val sorted = lessons.sortedBy { it.orderIndex }
            val current = sorted.getOrNull(lessonIndex)
            val next = sorted.getOrNull(lessonIndex + 1)
            if (current == null) {
                ScreenState.Error(message = Res.string.error_network)
            } else {
                ScreenState.Idle(
                    LessonCompleteUiState(
                        currentLetter = current.displayLetter,
                        // Từ đầu tiên CÓ HÌNH — ảnh WebP riêng cũng tính, không chỉ emoji.
                        currentWord = current.words.firstOrNull { it.displays.isNotEmpty() },
                        nextLetter = next?.displayLetter,
                    ),
                )
            }
        }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load lesson $lessonIndex of $unitId" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private companion object {
        const val TAG = "LessonCompleteViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class LessonCompleteUiState(
    val currentLetter: String,
    /** Từ để vẽ hình ăn mừng; null thì màn không hiện hình nào. */
    val currentWord: LessonWord?,
    val nextLetter: String?,
)
