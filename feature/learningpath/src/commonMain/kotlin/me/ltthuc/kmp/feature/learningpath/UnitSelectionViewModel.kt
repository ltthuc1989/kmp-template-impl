package me.ltthuc.kmp.feature.learningpath

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.model.UnitCard
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class UnitSelectionViewModel(
    private val levelId: String,
    levelRepository: LevelRepository,
    unitRepository: UnitRepository,
) : ViewModel() {

    private val levelFlow: Flow<Level?> = levelRepository.observeLevelCards().map { cards ->
        cards.firstOrNull { it.level.id == levelId }?.level
    }

    val screenState: StateFlow<ScreenState<UnitSelectionUiState>> = combine(
        levelFlow,
        unitRepository.observeUnitCards(levelId),
    ) { level, units ->
        if (level == null) {
            ScreenState.Error(message = Res.string.error_network)
        } else {
            val startedCount = units.count { it.totalStars > 0 }
            ScreenState.Idle(
                UnitSelectionUiState(
                    level = level,
                    units = units.toImmutableList(),
                    startedCount = startedCount,
                ),
            )
        }
    }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load unit selection for $levelId" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private companion object {
        const val TAG = "UnitSelectionViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class UnitSelectionUiState(
    val level: Level,
    val units: ImmutableList<UnitCard>,
    val startedCount: Int,
)
