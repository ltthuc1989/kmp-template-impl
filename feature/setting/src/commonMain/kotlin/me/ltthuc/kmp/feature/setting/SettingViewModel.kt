package me.ltthuc.kmp.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.common.share.Reviewer
import me.ltthuc.kmp.core.common.share.Sharer
import me.ltthuc.kmp.core.common.share.StoreLinks
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Language
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.model.Theme
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.ProgressResetRepository

class SettingViewModel(
    private val repository: AppSettingRepository,
    private val sharer: Sharer,
    private val reviewer: Reviewer,
    private val progressResetRepository: ProgressResetRepository,
    private val levelRepository: LevelRepository,
) : ViewModel() {
    val setting = repository.setting

    /** All levels in order, for the parent "unlock learning order" control. */
    val levels: StateFlow<List<Level>> = levelRepository.observeLevelCards()
        .map { cards -> cards.map { it.level } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun setAppThemePalette(palette: AppThemePalette) {
        viewModelScope.launch {
            repository.setAppThemePalette(palette)
        }
    }

    fun setDeveloperMode(developerMode: Boolean) {
        viewModelScope.launch {
            repository.setDeveloperMode(developerMode)
        }
    }

    fun setShowSpeakButton(value: Boolean) {
        viewModelScope.launch {
            repository.setShowSpeakButton(value)
        }
    }

    fun setSfxEnabled(value: Boolean) {
        viewModelScope.launch { repository.setSfxEnabled(value) }
    }

    fun setVoiceEnabled(value: Boolean) {
        viewModelScope.launch { repository.setVoiceEnabled(value) }
    }

    fun setMusicEnabled(value: Boolean) {
        viewModelScope.launch { repository.setMusicEnabled(value) }
    }

    /** Opens the native share sheet with the localized [invite] plus the store link. */
    fun shareApp(invite: String) {
        sharer.shareText("$invite\n${StoreLinks.PLAY_STORE_URL}")
    }

    /** Triggers the platform in-app review prompt (falls back to the store listing). */
    fun rateApp() {
        reviewer.requestReview()
    }

    /** Parent control: open every unit of an owned [levelId] at once (skip the sequential gate). */
    fun openLevelFully(levelId: String) {
        viewModelScope.launch { repository.addManualUnlock(levelId) }
    }

    /** Parent control: re-lock an owned [levelId] back to the sequential gate and reset its progress. */
    fun lockLevelSequential(levelId: String) {
        viewModelScope.launch {
            repository.removeManualUnlock(levelId)
            progressResetRepository.resetLevel(levelId)
        }
    }

    /** Wipes all local learning progress, then invokes [onDone] on completion. */
    fun resetProgress(onDone: () -> Unit) {
        viewModelScope.launch {
            progressResetRepository.resetAllProgress()
            onDone()
        }
    }
}
