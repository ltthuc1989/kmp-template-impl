package me.ltthuc.kmp.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import me.ltthuc.kmp.core.model.Language

/**
 * The resolved app language ("en" / "vi") chosen by the user — independent of the text-rendering
 * locale ([LocalAppLocale], which is forced to English on the kid-facing screens for immersion).
 *
 * Use this for:
 *  - voice prompts / guide audio (should follow the user's language everywhere, even on English UI),
 *  - the parent-facing screens (Settings, Parental Gate, Paywall, Onboarding) which override their
 *    own text locale back to this value.
 */
val LocalAppLanguage = staticCompositionLocalOf { "en" }

/** Resolve a [Language] choice to a concrete "en" / "vi" tag, falling back to [deviceTag] for System. */
fun resolveAppLanguage(language: Language, deviceTag: String): String = when (language) {
    Language.English -> "en"
    Language.Vietnamese -> "vi"
    Language.System -> if (deviceTag.startsWith("vi")) "vi" else "en"
}
