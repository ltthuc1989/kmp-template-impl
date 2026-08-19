package me.ltthuc.kmp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import io.github.vinceglb.filekit.coil.addPlatformFileSupport
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.GrabeeTheme
import org.koin.compose.koinInject

/**
 * @param overlay platform chrome drawn above the nav host and inside [GrabeeTheme], so it can
 *   use the app's colours and locale. Android passes the in-app update banner here; iOS has
 *   no equivalent and leaves it empty. Keeping it a slot is what allows the Play update API,
 *   which is Android-only, to stay out of commonMain.
 */
@Composable
internal fun GrabeeApp(
    setting: AppSetting,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
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

    GrabeeTheme(setting) {
        // Khôi phục màn cuối khi mở lại app: trong unit → Lesson Map, Unit list → Unit list.
        // Người mới (NONE) rơi vào nhánh cuối: màn chọn level.
        val level = setting.lastLevelId.ifBlank { "L1" }
        val starts: List<Destination> = when {
            !setting.hasSeenOnboarding -> listOf(Destination.Onboarding)
            setting.lastScreen == AppSetting.LastScreen.UNIT_LIST ->
                listOf(Destination.Learning.UnitSelection(level))
            setting.lastScreen == AppSetting.LastScreen.LESSON_MAP && setting.lastUnitId.isNotBlank() ->
                listOf(
                    Destination.Learning.UnitSelection(level),
                    Destination.Learning.LessonMap(level, setting.lastUnitId),
                )
            // Ship cả Level 1 và Level 2 nên phải cho chọn. Trước đây app ghim thẳng vào bản
            // đồ L1 tới khi học xong L1 — hợp lý khi chỉ có một level, còn bây giờ là giấu
            // mất một nửa nội dung. Bỏ luôn được cả bước chờ truy vấn "L1 xong chưa" trước
            // khi dựng backstack, nên app mở nhanh hơn một nhịp.
            else -> listOf(Destination.Home)
        }
        Box(modifier = modifier) {
            AppNavHost(
                startDestinations = starts,
                modifier = Modifier.fillMaxSize(),
            )
            overlay()
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
