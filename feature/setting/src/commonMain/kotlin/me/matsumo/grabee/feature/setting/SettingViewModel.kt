package me.matsumo.grabee.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.matsumo.grabee.core.model.AppThemePalette
import me.matsumo.grabee.core.model.Theme
import me.matsumo.grabee.core.repository.AppSettingRepository

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
}
