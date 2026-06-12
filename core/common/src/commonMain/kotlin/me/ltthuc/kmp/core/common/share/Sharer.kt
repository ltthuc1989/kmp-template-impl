package me.ltthuc.kmp.core.common.share

/** Opens the native share sheet to share [text] (e.g. an app invite + store link). */
expect class Sharer {
    fun shareText(text: String)
}
