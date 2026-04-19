package me.matsumo.grabee.feature.home

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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.matsumo.grabee.core.model.LevelCard
import me.matsumo.grabee.core.repository.LevelRepository
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.error_network
import me.matsumo.grabee.core.ui.screen.ScreenState

internal class HomeViewModel(
    levelRepository: LevelRepository,
) : ViewModel() {

    private val levelCards: Flow<List<LevelCard>> = levelRepository.observeLevelCards()

    val screenState: StateFlow<ScreenState<HomeUiState>> = levelCards
        .map<List<LevelCard>, ScreenState<HomeUiState>> { cards ->
            ScreenState.Idle(HomeUiState(levels = cards.toImmutableList()))
        }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load level cards" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private companion object {
        const val TAG = "HomeViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class HomeUiState(
    val levels: ImmutableList<LevelCard>,
)
