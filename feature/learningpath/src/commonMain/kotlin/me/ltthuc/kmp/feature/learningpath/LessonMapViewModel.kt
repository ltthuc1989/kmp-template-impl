package me.ltthuc.kmp.feature.learningpath

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.ltthuc.kmp.core.model.LessonCard
import me.ltthuc.kmp.core.model.LessonStatus
import me.ltthuc.kmp.core.model.PhonicsUnit
import me.ltthuc.kmp.core.repository.UnitCompletionRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class LessonMapViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    unitCompletionRepository: UnitCompletionRepository,
) : ViewModel() {

    val screenState: StateFlow<ScreenState<LessonMapUiState>> = combine(
        unitRepository.observeUnit(unitId),
        unitRepository.observeLessonCards(unitId),
        unitCompletionRepository.observeCount(unitId),
    ) { unit, lessons, completionCount ->
        if (unit == null) {
            ScreenState.Error(message = Res.string.error_network)
        } else {
            val allComplete = lessons.isNotEmpty() && lessons.all { it.status == LessonStatus.Completed }
            ScreenState.Idle(
                LessonMapUiState(
                    unit = unit,
                    lessons = lessons.toImmutableList(),
                    allLessonsComplete = allComplete,
                    completionCount = completionCount,
                ),
            )
        }
    }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load lesson map for $unitId" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private companion object {
        const val TAG = "LessonMapViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class LessonMapUiState(
    val unit: PhonicsUnit,
    val lessons: ImmutableList<LessonCard>,
    val allLessonsComplete: Boolean,
    val completionCount: Int,
)
