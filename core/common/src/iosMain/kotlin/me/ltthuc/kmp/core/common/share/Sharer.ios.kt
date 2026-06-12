package me.ltthuc.kmp.core.common.share

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

actual class Sharer {
    actual fun shareText(text: String) {
        val controller = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null,
        )
        val root = UIApplication.sharedApplication.keyWindow?.rootViewController
        root?.presentViewController(controller, animated = true, completion = null)
    }
}
