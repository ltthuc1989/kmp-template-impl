package me.matsumo.grabee.feature.learningpath.step.identify

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
import me.matsumo.grabee.core.model.PhonicsLesson
import me.matsumo.grabee.core.repository.UnitRepository
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.error_network
import me.matsumo.grabee.core.resource.error_no_data
import me.matsumo.grabee.core.ui.screen.ScreenState

internal class IdentifyViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
) : ViewModel() {

    val screenState: StateFlow<ScreenState<IdentifyUiState>> =
        unitRepository.observeLessons(unitId)
            .map { lessons ->
                if (lessons.isEmpty()) {
                    ScreenState.Error(message = Res.string.error_no_data)
                } else {
                    ScreenState.Idle(IdentifyUiState(lessons = lessons.toImmutableList()))
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
        const val TAG = "IdentifyViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class IdentifyUiState(
    val lessons: ImmutableList<PhonicsLesson>,
)
