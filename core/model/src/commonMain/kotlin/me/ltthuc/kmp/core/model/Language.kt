package me.ltthuc.kmp.core.model

enum class Language {
    /** Follow the device locale (no override). VN devices get Vietnamese; everything else falls back to English. */
    System,
    English,
    Vietnamese,
    ;

    /** BCP-47 tag used to override [androidx.compose.ui.text.intl.Locale] at runtime. `null` = follow device. */
    fun toBcp47Tag(): String? = when (this) {
        System -> null
        English -> "en"
        Vietnamese -> "vi"
    }
}
