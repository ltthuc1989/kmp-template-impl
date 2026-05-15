package me.ltthuc.kmp.feature.learningpath

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class LevelCompleteViewModel(
    private val levelId: String,
    levelRepository: LevelRepository,
    unitRepository: UnitRepository,
) : ViewModel() {

    val state: StateFlow<ScreenState<LevelCompleteUiState>> = combine(
        levelRepository.observeLevel(levelId),
        unitRepository.observeUnits(levelId),
    ) { level, units ->
        if (level == null || units.isEmpty()) {
            ScreenState.Error(message = Res.string.error_network)
        } else {
            ScreenState.Idle(
                LevelCompleteUiState(
                    levelTitle = level.title,
                    totalUnits = units.size,
                    firstUnitId = units.first().id,
                ),
            )
        }
    }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load LevelComplete state for $levelId" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private companion object {
        const val TAG = "LevelCompleteViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class LevelCompleteUiState(
    val levelTitle: String,
    val totalUnits: Int,
    val firstUnitId: String,
)
