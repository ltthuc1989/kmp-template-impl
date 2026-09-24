package me.ltthuc.kmp

import android.app.Application
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import me.ltthuc.kmp.di.applyModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class GrabeeApplication : Application(), KoinStartup {
    override fun onCreate() {
        super.onCreate()

        // Log ở CẢ bản release (playbook R4). Bản debug là bản ta ngồi cạnh, cắm cáp, đặt
        // breakpoint được; bản release chạy trên máy người lạ. Tắt log đúng ở đó là tự bịt mắt
        // lúc mù nhất -- đã hai lần chặn việc truy một lỗi tải chỉ xảy ra sau khi R8 chạy.
        // Release giữ từ INFO trở lên; app không ghi PII vào log nên không có gì để rò rỉ.
        Napier.base(if (BuildConfig.DEBUG) DebugAntilog() else ReleaseAntilog())
        // StrictMode.enableDefaults()
    }

    override fun onKoinStartup() = koinConfiguration {
        androidContext(this@GrabeeApplication)
        androidLogger()
        applyModules()
    }
}

/** Release logging: INFO and above, straight to Logcat, no PII. */
private class ReleaseAntilog : Antilog() {
    override fun performLog(
        priority: LogLevel,
        tag: String?,
        throwable: Throwable?,
        message: String?,
    ) {
        if (priority < LogLevel.INFO) return
        val label = tag ?: "Grabee"
        val body = message ?: throwable?.message.orEmpty()
        when (priority) {
            LogLevel.INFO -> android.util.Log.i(label, body, throwable)
            LogLevel.WARNING -> android.util.Log.w(label, body, throwable)
            LogLevel.ERROR -> android.util.Log.e(label, body, throwable)
            LogLevel.ASSERT -> android.util.Log.wtf(label, body, throwable)
            else -> android.util.Log.i(label, body, throwable)
        }
    }
}
