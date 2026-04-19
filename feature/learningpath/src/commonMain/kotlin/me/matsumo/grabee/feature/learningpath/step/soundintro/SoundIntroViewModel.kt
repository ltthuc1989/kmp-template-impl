package me.matsumo.grabee.feature.learningpath.step.soundintro

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.matsumo.grabee.core.model.Word
import me.matsumo.grabee.core.repository.UnitRepository
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.error_network
import me.matsumo.grabee.core.ui.screen.ScreenState

internal class SoundIntroViewModel(
    private val unitId: String,
    unitRepository: UnitRepository,
) : ViewModel() {

    private val currentWordIndex = MutableStateFlow(0)

    val screenState: StateFlow<ScreenState<SoundIntroUiState>> = combine(
        unitRepository.observeWords(unitId).map { it.toImmutableList() },
        currentWordIndex,
    ) { words, index ->
        if (words.isEmpty()) {
            ScreenState.Error(message = Res.string.error_no_data)
        } else {
            val safeIndex = index.coerceIn(0, words.lastIndex)
            ScreenState.Idle(
                SoundIntroUiState(
                    words = words,
                    currentIndex = safeIndex,
                ),
            )
        }
    }
        .catch { throwable ->
            Napier.e(tag = TAG, throwable = throwable) { "Failed to load sound intro words for $unitId" }
            emit(ScreenState.Error(message = Res.string.error_network))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
            initialValue = ScreenState.Loading(),
        )

    fun moveNext(): Boolean {
        val state = screenState.value
        if (state !is ScreenState.Idle) return false
        val ui = state.data
        val next = ui.currentIndex + 1
        return if (next <= ui.words.lastIndex) {
            currentWordIndex.value = next
            true
        } else {
            false
        }
    }

    private companion object {
        const val TAG = "SoundIntroViewModel"
        const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
    }
}

@Immutable
internal data class SoundIntroUiState(
    val words: ImmutableList<Word>,
    val currentIndex: Int,
) {
    val currentWord: Word get() = words[currentIndex]
    val totalWords: Int get() = words.size
}
