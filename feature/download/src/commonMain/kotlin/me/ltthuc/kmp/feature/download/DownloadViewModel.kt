package me.ltthuc.kmp.feature.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.repository.ContentPackRepository

/** What the download screen shows. Mirrors the pack lifecycle, not the network. */
sealed interface DownloadUiState {
    data object Loading : DownloadUiState

    /** Nothing to fetch — the level already works offline. */
    data object Ready : DownloadUiState

    data class Pending(val bytes: Long) : DownloadUiState

    data class Running(val filesDone: Int, val filesTotal: Int, val fraction: Float) : DownloadUiState

    data class Failed(val bytes: Long) : DownloadUiState
}

/**
 * Drives downloading every content pack of one level.
 *
 * Packs are downloaded in order and reported as one job, because "unit 3 of 6" is not a
 * unit of progress a parent cares about — they asked for the level. Cancelling (leaving the
 * screen) keeps finished files, so coming back resumes rather than restarts.
 */
class DownloadViewModel(
    private val levelId: String,
    private val packRepository: ContentPackRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DownloadUiState>(DownloadUiState.Loading)
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val pending = packRepository.pendingBytesForLevel(levelId)
            _uiState.value = if (pending == 0L) DownloadUiState.Ready else DownloadUiState.Pending(pending)
        }
    }

    fun start() {
        if (downloadJob?.isActive == true) return

        downloadJob = viewModelScope.launch {
            val packIds = packRepository.packIdsForLevel(levelId)
            // Totals across the whole level so the bar does not restart at every pack.
            var filesDoneBefore = 0

            runCatching {
                for (packId in packIds) {
                    var lastTotal = 0
                    packRepository.download(packId).collectLatest { progress ->
                        lastTotal = progress.filesTotal
                        val done = filesDoneBefore + progress.filesDone
                        val total = (filesDoneBefore + progress.filesTotal).coerceAtLeast(1)
                        _uiState.value = DownloadUiState.Running(
                            filesDone = done,
                            filesTotal = total,
                            fraction = done.toFloat() / total,
                        )
                    }
                    filesDoneBefore += lastTotal
                }
            }.onSuccess {
                _uiState.value = DownloadUiState.Ready
            }.onFailure { cause ->
                Napier.e("Level $levelId download failed", cause)
                _uiState.value = DownloadUiState.Failed(packRepository.pendingBytesForLevel(levelId))
            }
        }
    }
}
