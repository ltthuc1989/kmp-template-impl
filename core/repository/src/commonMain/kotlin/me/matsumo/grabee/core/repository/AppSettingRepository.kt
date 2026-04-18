package me.matsumo.grabee.core.repository

import kotlinx.coroutines.flow.StateFlow
import me.matsumo.grabee.core.datasource.AppSettingDataSource
import me.matsumo.grabee.core.model.AppSetting
import me.matsumo.grabee.core.model.AppThemePalette
import me.matsumo.grabee.core.model.Theme

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
}
