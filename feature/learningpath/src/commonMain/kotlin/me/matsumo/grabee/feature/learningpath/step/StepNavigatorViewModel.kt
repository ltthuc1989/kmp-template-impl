package me.matsumo.grabee.feature.learningpath.step

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.matsumo.grabee.core.repository.UnitRepository

internal class StepNavigatorViewModel(
    unitId: String,
    unitRepository: UnitRepository,
) : ViewModel() {

    val totalWords: StateFlow<Int> = unitRepository.observeWords(unitId)
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = 0,
        )

    private companion object {
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}
