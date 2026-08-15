package me.ltthuc.kmp.core.datasource

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import me.ltthuc.kmp.core.datasource.helper.PreferenceHelper
import me.ltthuc.kmp.core.datasource.helper.deserialize
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Language
import me.ltthuc.kmp.core.model.Theme
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AppSettingDataSource(
    private val preferenceHelper: PreferenceHelper,
    private val formatter: Json,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val preference = preferenceHelper.create(PreferencesName.SETTING)

    val setting = preference.data.map {
        it.deserialize(formatter, AppSetting.serializer(), AppSetting.DEFAULT)
    }.stateIn(
        scope = CoroutineScope(ioDispatcher),
        started = SharingStarted.WhileSubscribed(1000),
        initialValue = AppSetting.DEFAULT,
    )

    @OptIn(ExperimentalUuidApi::class)
    suspend fun initializeIdIfNeeded() = withContext(ioDispatcher) {
        val current = setting.first()
        if (current.id.isBlank()) {
            val uuid = Uuid.random().toString()
            preference.edit {
                it[stringPreferencesKey(AppSetting::id.name)] = uuid
            }
        }
    }

    suspend fun setId(id: String) = withContext(ioDispatcher) {
        if (setting.first().id == id) return@withContext

        preference.edit {
            it[stringPreferencesKey(AppSetting::id.name)] = id
        }
    }

    suspend fun setTheme(theme: Theme) = withContext(ioDispatcher) {
        if (setting.first().theme == theme) return@withContext

        preference.edit {
            it[stringPreferencesKey(AppSetting::theme.name)] = theme.name
        }
    }

    suspend fun setLanguage(language: Language) = withContext(ioDispatcher) {
        if (setting.first().language == language) return@withContext

        preference.edit {
            it[stringPreferencesKey(AppSetting::language.name)] = language.name
        }
    }

    suspend fun setAppThemePalette(palette: AppThemePalette) = withContext(ioDispatcher) {
        if (setting.first().appThemePalette == palette) return@withContext

        preference.edit {
            it[stringPreferencesKey(AppSetting::appThemePalette.name)] = palette.name
        }
    }

    suspend fun setPlusMode(plusMode: Boolean) = withContext(ioDispatcher) {
        if (setting.first().plusMode == plusMode) return@withContext

        preference.edit {
            it[booleanPreferencesKey(AppSetting::plusMode.name)] = plusMode
        }
    }

    suspend fun setOwnedLevelIds(ids: Set<String>) = withContext(ioDispatcher) {
        if (setting.first().ownedLevelIds == ids) return@withContext
        preference.edit {
            it[stringPreferencesKey(AppSetting::ownedLevelIds.name)] =
                formatter.encodeToString(SetSerializer(String.serializer()), ids)
        }
    }

    suspend fun setManualUnlockedLevelIds(ids: Set<String>) = withContext(ioDispatcher) {
        if (setting.first().manualUnlockedLevelIds == ids) return@withContext
        preference.edit {
            it[stringPreferencesKey(AppSetting::manualUnlockedLevelIds.name)] =
                formatter.encodeToString(SetSerializer(String.serializer()), ids)
        }
    }

    suspend fun setAdUnlockedLevelIds(ids: Set<String>) = withContext(ioDispatcher) {
        if (setting.first().adUnlockedLevelIds == ids) return@withContext
        preference.edit {
            it[stringPreferencesKey(AppSetting::adUnlockedLevelIds.name)] =
                formatter.encodeToString(SetSerializer(String.serializer()), ids)
        }
    }

    suspend fun setDeveloperMode(developerMode: Boolean) = withContext(ioDispatcher) {
        if (setting.first().developerMode == developerMode) return@withContext

        preference.edit {
            it[booleanPreferencesKey(AppSetting::developerMode.name)] = developerMode
        }
    }

    suspend fun setHasSeenOnboarding(hasSeenOnboarding: Boolean) = withContext(ioDispatcher) {
        if (setting.first().hasSeenOnboarding == hasSeenOnboarding) return@withContext

        preference.edit {
            it[booleanPreferencesKey(AppSetting::hasSeenOnboarding.name)] = hasSeenOnboarding
        }
    }

    suspend fun setShowSpeakButton(value: Boolean) = withContext(ioDispatcher) {
        if (setting.first().showSpeakButton == value) return@withContext

        preference.edit {
            it[booleanPreferencesKey(AppSetting::showSpeakButton.name)] = value
        }
    }

    suspend fun setSfxEnabled(value: Boolean) = withContext(ioDispatcher) {
        if (setting.first().sfxEnabled == value) return@withContext
        preference.edit { it[booleanPreferencesKey(AppSetting::sfxEnabled.name)] = value }
    }

    suspend fun setVoiceEnabled(value: Boolean) = withContext(ioDispatcher) {
        if (setting.first().voiceEnabled == value) return@withContext
        preference.edit { it[booleanPreferencesKey(AppSetting::voiceEnabled.name)] = value }
    }

    suspend fun setMusicEnabled(value: Boolean) = withContext(ioDispatcher) {
        if (setting.first().musicEnabled == value) return@withContext
        preference.edit { it[booleanPreferencesKey(AppSetting::musicEnabled.name)] = value }
    }

    suspend fun setGlobalMuted(value: Boolean) = withContext(ioDispatcher) {
        if (setting.first().globalMuted == value) return@withContext
        preference.edit { it[booleanPreferencesKey(AppSetting::globalMuted.name)] = value }
    }

    suspend fun setLastScreen(
        screen: AppSetting.LastScreen,
        levelId: String,
        unitId: String,
    ) = withContext(ioDispatcher) {
        val cur = setting.first()
        if (cur.lastScreen == screen && cur.lastLevelId == levelId && cur.lastUnitId == unitId) {
            return@withContext
        }
        preference.edit {
            it[stringPreferencesKey(AppSetting::lastScreen.name)] = screen.name
            it[stringPreferencesKey(AppSetting::lastLevelId.name)] = levelId
            it[stringPreferencesKey(AppSetting::lastUnitId.name)] = unitId
        }
    }
}
