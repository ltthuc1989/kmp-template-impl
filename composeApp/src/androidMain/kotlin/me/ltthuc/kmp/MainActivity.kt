package me.ltthuc.kmp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import me.ltthuc.kmp.core.common.share.AndroidActivityHolder
import me.ltthuc.kmp.core.model.Theme
import me.ltthuc.kmp.core.ui.theme.shouldUseDarkTheme
import me.ltthuc.kmp.update.InAppUpdateController
import me.ltthuc.kmp.update.UpdateReadyBanner
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel by viewModel<MainViewModel>()

    // Built here rather than injected: it registers an activity result launcher, which
    // ComponentActivity only accepts before the activity is STARTED, and it observes this
    // activity's lifecycle to re-check on every resume.
    private lateinit var inAppUpdate: InAppUpdateController

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        inAppUpdate = InAppUpdateController(this)
        enableEdgeToEdge()
        setContent {
            val userData by viewModel.setting.collectAsStateWithLifecycle(null)
            val isSystemInDarkTheme = shouldUseDarkTheme(userData?.theme ?: Theme.System)

            val lightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
            val darkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

            DisposableEffect(isSystemInDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { isSystemInDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(lightScrim, darkScrim) { isSystemInDarkTheme },
                )
                onDispose {}
            }

            val updateReady by inAppUpdate.readyToInstall.collectAsStateWithLifecycle()

            userData?.let {
                GrabeeApp(
                    modifier = Modifier.fillMaxSize(),
                    setting = it,
                    overlay = {
                        UpdateReadyBanner(
                            visible = updateReady,
                            onRestart = inAppUpdate::completeUpdate,
                        )
                    },
                )
            }

            splashScreen.setKeepOnScreenCondition { userData == null }
        }

        FileKit.init(this)
    }

    override fun onResume() {
        super.onResume()
        AndroidActivityHolder.current = this
    }

    override fun onDestroy() {
        if (AndroidActivityHolder.current === this) {
            AndroidActivityHolder.current = null
        }
        super.onDestroy()
    }
}
