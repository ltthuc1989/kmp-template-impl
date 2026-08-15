package me.ltthuc.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.GrabeeTheme
import org.koin.compose.koinInject

@Composable
internal fun GrabeeApp(
    setting: AppSetting,
    modifier: Modifier = Modifier,
) {
    SetupCoil()

    // Mirror user audio settings into the SFX layer. Cheap (writes to MutableStateFlow)
    // so we run it on every recomposition keyed by the relevant fields.
    val sfx = koinInject<SfxController>()
    LaunchedEffect(setting.sfxEnabled, setting.voiceEnabled, setting.musicEnabled, setting.globalMuted) {
        sfx.configure(
            sfxEnabled = setting.sfxEnabled,
            voiceEnabled = setting.voiceEnabled,
            musicEnabled = setting.musicEnabled,
            globalMuted = setting.globalMuted,
        )
    }

    // Phase này chỉ ship Level 1: vào thẳng bản đồ L1 cho tới khi xong L1, sau đó mới là Home (5-level).
    val levelRepository = koinInject<LevelRepository>()
    val isLevel1Complete by levelRepository.observeIsLevelComplete("L1")
        .collectAsStateWithLifecycle(null)

    GrabeeTheme(setting) {
        // Chờ biết trạng thái L1 rồi mới khởi tạo backstack (tránh chọn sai start).
        val complete = isLevel1Complete
        if (complete != null) {
            // Khôi phục màn cuối khi mở lại app: trong unit → Lesson Map, Unit list → Unit list,
            // Level list → Home. User mới (NONE) dùng mặc định theo trạng thái L1.
            val level = setting.lastLevelId.ifBlank { "L1" }
            val starts: List<Destination> = when {
                !setting.hasSeenOnboarding -> listOf(Destination.Onboarding)
                // Developer mode ships every level, so start on the level list. Without this the
                // ship-L1-first rule below pins the app to the L1 unit map and there is no way to
                // reach L2+ at all — back from the unit map exits the app.
                setting.developerMode -> listOf(Destination.Home)
                setting.lastScreen == AppSetting.LastScreen.LEVEL_LIST -> listOf(Destination.Home)
                setting.lastScreen == AppSetting.LastScreen.UNIT_LIST ->
                    listOf(Destination.Learning.UnitSelection(level))
                setting.lastScreen == AppSetting.LastScreen.LESSON_MAP && setting.lastUnitId.isNotBlank() ->
                    listOf(
                        Destination.Learning.UnitSelection(level),
                        Destination.Learning.LessonMap(level, setting.lastUnitId),
                    )
                complete -> listOf(Destination.Home)
                else -> listOf(Destination.Learning.UnitSelection("L1"))
            }
            AppNavHost(
                startDestinations = starts,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SetupCoil() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                addPlatformFileSupport()
            }
            .crossfade(true)
            .build()
    }
}
