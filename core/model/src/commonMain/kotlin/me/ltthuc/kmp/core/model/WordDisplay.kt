package me.ltthuc.kmp.core.model

sealed interface WordDisplay {
    data class Emoji(val char: String) : WordDisplay
    data class Image(val path: String) : WordDisplay
}
