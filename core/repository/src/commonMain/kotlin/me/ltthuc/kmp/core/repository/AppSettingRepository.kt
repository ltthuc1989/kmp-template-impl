package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.StateFlow
import me.ltthuc.kmp.core.datasource.AppSettingDataSource
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Theme

class AppSettingRepository(
    private val dataSource: AppSettingDataSource,
) {
    val setting: StateFlow<AppSetting> = dataSource.setting

    suspend fun initializeIdIfNeeded() = dataSource.initializeIdIfNeeded()

    suspend fun setId(id: String) = dataSource.setId(id)

    suspend fun setTheme(theme: Theme) = dataSource.setTheme(theme)

    suspend fun setAppThemePalette(palette: AppThemePalette) = dataSource.setAppThemePalette(palette)

    suspend fun setPlusMode(plusMode: Boolean) = dataSource.setPlusMode(plusMode)

    suspend fun setDeveloperMode(developerMode: Boolean) = dataSource.setDeveloperMode(developerMode)

    suspend fun setHasSeenOnboarding(hasSeenOnboarding: Boolean) = dataSource.setHasSeenOnboarding(hasSeenOnboarding)

    suspend fun setShowSpeakButton(value: Boolean) = dataSource.setShowSpeakButton(value)

    suspend fun setSfxEnabled(value: Boolean) = dataSource.setSfxEnabled(value)

    suspend fun setVoiceEnabled(value: Boolean) = dataSource.setVoiceEnabled(value)

    suspend fun setMusicEnabled(value: Boolean) = dataSource.setMusicEnabled(value)

    suspend fun setGlobalMuted(value: Boolean) = dataSource.setGlobalMuted(value)
}
