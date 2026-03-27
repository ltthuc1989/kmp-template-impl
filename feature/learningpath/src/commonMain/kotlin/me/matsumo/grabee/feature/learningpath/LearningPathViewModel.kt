package me.matsumo.grabee.feature.learningpath

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.matsumo.grabee.core.common.suspendRunCatching
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.error_network
import me.matsumo.grabee.core.ui.screen.ScreenState

internal class LearningPathViewModel : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState<LearningPathUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<LearningPathUiState>> = _screenState.asStateFlow()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading()
            _screenState.value = suspendRunCatching {
                buildSampleData()
            }.fold(
                onSuccess = { ScreenState.Idle(it) },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    private fun buildSampleData(): LearningPathUiState {
        return LearningPathUiState(
            activeLevel = LevelData(
                number = 1,
                name = "Alphabet",
                totalUnits = 8,
                completedUnits = 2,
                visibleUnits = persistentListOf(
                    UnitData(1, "Aa, Bb, Cc", UnitState.COMPLETED),
                    UnitData(2, "Dd, Ee, Ff", UnitState.ACTIVE, 0.45f),
                    UnitData(3, "Gg, Hh, Ii", UnitState.LOCKED),
                    UnitData(4, "Jj, Kk, Ll", UnitState.LOCKED),
                    UnitData(5, "Mm, Nn, Oo", UnitState.LOCKED),
                ),
                hiddenUnitCount = 3,
            ),
            nextLevel = LevelData(
                number = 2,
                name = "Common Phrases",
                totalUnits = 8,
                completedUnits = 0,
                visibleUnits = persistentListOf(),
                isLocked = true,
                estimatedHours = 4,
            ),
            futureGoals = persistentListOf(
                LevelData(
                    number = 3,
                    name = "Sentence Structure",
                    totalUnits = 0,
                    completedUnits = 0,
                    visibleUnits = persistentListOf(),
                    isLocked = true,
                ),
            ),
            smallLevels = persistentListOf(
                LevelData(4, "Level 4", 0, 0, persistentListOf(), isLocked = true),
                LevelData(5, "Level 5", 0, 0, persistentListOf(), isLocked = true),
            ),
            streakDays = 12,
            rank = "Gold League",
            rankPosition = 4,
            isRankUp = true,
        )
    }
}

@Stable
data class LearningPathUiState(
    val activeLevel: LevelData,
    val nextLevel: LevelData?,
    val futureGoals: ImmutableList<LevelData>,
    val smallLevels: ImmutableList<LevelData>,
    val streakDays: Int,
    val rank: String,
    val rankPosition: Int,
    val isRankUp: Boolean,
)

@Stable
data class LevelData(
    val number: Int,
    val name: String,
    val totalUnits: Int,
    val completedUnits: Int,
    val visibleUnits: ImmutableList<UnitData>,
    val hiddenUnitCount: Int = 0,
    val isLocked: Boolean = false,
    val estimatedHours: Int? = null,
)

@Stable
data class UnitData(
    val number: Int,
    val letters: String,
    val state: UnitState,
    val progress: Float? = null,
)

enum class UnitState { COMPLETED, ACTIVE, LOCKED }
