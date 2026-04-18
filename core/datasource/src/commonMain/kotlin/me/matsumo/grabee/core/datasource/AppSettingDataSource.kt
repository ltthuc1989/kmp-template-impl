package me.matsumo.grabee.core.datasource

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
import kotlinx.serialization.json.Json
import me.matsumo.grabee.core.datasource.helper.PreferenceHelper
import me.matsumo.grabee.core.datasource.helper.deserialize
import me.matsumo.grabee.core.model.AppSetting
import me.matsumo.grabee.core.model.AppThemePalette
import me.matsumo.grabee.core.model.Theme
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
}
