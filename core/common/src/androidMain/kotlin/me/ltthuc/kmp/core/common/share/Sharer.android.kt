package me.ltthuc.kmp.core.common.share

import android.content.Context
import android.content.Intent

actual class Sharer(private val context: Context) {
    actual fun shareText(text: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        (AndroidActivityHolder.current ?: context).startActivity(chooser)
    }
}
