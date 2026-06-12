package me.ltthuc.kmp.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.common.share.Reviewer
import me.ltthuc.kmp.core.common.share.Sharer
import me.ltthuc.kmp.core.common.share.StoreLinks
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Theme
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.ProgressResetRepository

class SettingViewModel(
    private val repository: AppSettingRepository,
    private val sharer: Sharer,
    private val reviewer: Reviewer,
    private val progressResetRepository: ProgressResetRepository,
) : ViewModel() {
    val setting = repository.setting

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            repository.setTheme(theme)
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

    /** Wipes all local learning progress, then invokes [onDone] on completion. */
    fun resetProgress(onDone: () -> Unit) {
        viewModelScope.launch {
            progressResetRepository.resetAllProgress()
            onDone()
        }
    }
}
