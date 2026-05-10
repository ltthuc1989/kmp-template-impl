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
import me.ltthuc.kmp.core.model.PhonicsUnit
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitCompletionRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.ui.screen.ScreenState

internal class UnitCompleteViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
    levelRepository: LevelRepository,
    unitCompletionRepository: UnitCompletionRepository,
) : ViewModel() {

    val state: StateFlow<ScreenState<UnitCompleteUiState>> = combine(
        unitRepository.observeUnit(unitId),
        unitRepository.observeLessons(unitId),
        unitCompletionRepository.observeCount(unitId),
        levelRepository.observeNextUnit(unitId),
        unitCompletionRepository.observeAll(),
    ) { unit, lessons, count, nextUnit, completions ->
        if (unit == null) {
            ScreenState.Error(message = Res.string.error_network)
        } else {
            // 1 emoji per lesson — first non-null word emoji of each lesson, in lesson order.
            val emojis = lessons
                .mapNotNull { lesson -> lesson.words.firstNotNullOfOrNull { it.emoji } }
                .take(MAX_EMOJIS)
                .toImmutableList()
            // Distinct completed units across whole catalogue; drives whether "Pick another
            // letter" CTA is shown (only meaningful when ≥ 2 units done = ≥ 6 lessons unlocked).
            val completedUnitCount = completions.count { it.completionCount > 0 }
            ScreenState.Idle(
                UnitCompleteUiState(
                    unit = unit,
                    emojis = emojis,
                    completionCount = count.coerceAtLeast(1),
                    completedUnitCount = completedUnitCount,
                    nextUnit = nextUnit,
                ),
            )
        }
    }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load UnitComplete state for $unitId" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    private companion object {
        const val TAG = "UnitCompleteViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
        const val MAX_EMOJIS = 3
    }
}

@Immutable
internal data class UnitCompleteUiState(
    val unit: PhonicsUnit,
    val emojis: ImmutableList<String>,
    val completionCount: Int,
    val completedUnitCount: Int,
    val nextUnit: PhonicsUnit?,
)
