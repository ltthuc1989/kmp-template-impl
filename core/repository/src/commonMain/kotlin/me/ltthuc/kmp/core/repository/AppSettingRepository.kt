package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.StateFlow
import me.ltthuc.kmp.core.datasource.AppSettingDataSource
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Language
import me.ltthuc.kmp.core.model.Theme

class AppSettingRepository(
    private val dataSource: AppSettingDataSource,
) {
    val setting: StateFlow<AppSetting> = dataSource.setting

    suspend fun initializeIdIfNeeded() = dataSource.initializeIdIfNeeded()

    suspend fun setId(id: String) = dataSource.setId(id)

    suspend fun setTheme(theme: Theme) = dataSource.setTheme(theme)

    suspend fun setLanguage(language: Language) = dataSource.setLanguage(language)

    suspend fun setAppThemePalette(palette: AppThemePalette) = dataSource.setAppThemePalette(palette)

    suspend fun setPlusMode(plusMode: Boolean) = dataSource.setPlusMode(plusMode)

    suspend fun setDeveloperMode(developerMode: Boolean) = dataSource.setDeveloperMode(developerMode)

    suspend fun setOwnedLevelIds(ids: Set<String>) = dataSource.setOwnedLevelIds(ids)

    /** Dev/test helper: drop all owned levels so the paywall flow can be retried. */
    suspend fun clearOwnedLevels() = dataSource.setOwnedLevelIds(emptySet())

    suspend fun addManualUnlock(levelId: String) =
        dataSource.setManualUnlockedLevelIds(setting.value.manualUnlockedLevelIds + levelId)

    suspend fun removeManualUnlock(levelId: String) =
        dataSource.setManualUnlockedLevelIds(setting.value.manualUnlockedLevelIds - levelId)

    suspend fun setHasSeenOnboarding(hasSeenOnboarding: Boolean) = dataSource.setHasSeenOnboarding(hasSeenOnboarding)

    suspend fun setShowSpeakButton(value: Boolean) = dataSource.setShowSpeakButton(value)

    suspend fun setSfxEnabled(value: Boolean) = dataSource.setSfxEnabled(value)

    suspend fun setVoiceEnabled(value: Boolean) = dataSource.setVoiceEnabled(value)

    suspend fun setMusicEnabled(value: Boolean) = dataSource.setMusicEnabled(value)

    suspend fun setGlobalMuted(value: Boolean) = dataSource.setGlobalMuted(value)

    suspend fun setLastScreen(screen: AppSetting.LastScreen, levelId: String, unitId: String) =
        dataSource.setLastScreen(screen, levelId, unitId)
}
