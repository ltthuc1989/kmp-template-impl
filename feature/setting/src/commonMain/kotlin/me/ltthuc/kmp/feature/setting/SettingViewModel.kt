package me.ltthuc.kmp.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Theme
import me.ltthuc.kmp.core.repository.AppSettingRepository

class SettingViewModel(
    private val repository: AppSettingRepository,
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
}
