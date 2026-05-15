package me.ltthuc.kmp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSetting(
    val id: String,
    val theme: Theme,
    val appThemePalette: AppThemePalette,
    val plusMode: Boolean,
    val developerMode: Boolean,
    val hasSeenOnboarding: Boolean,
    val practiceRoundMultiplier: Float = 1f,
    val showSpeakButton: Boolean = false,
    // UI sound effects (chime/click/lesson_complete).
    val sfxEnabled: Boolean = true,
    // Voice praise / nudge ("Great job!", "Try again!"). MUST NOT gate lesson phoneme audio.
    val voiceEnabled: Boolean = true,
    // Background music loop (Home/UnitSelection).
    val musicEnabled: Boolean = true,
    // Khan Kids-style master mute. Overrides the three flags above when true.
    val globalMuted: Boolean = false,
) {
    val hasPrivilege get() = plusMode || developerMode

    companion object {
        val DEFAULT = AppSetting(
            id = "",
            theme = Theme.System,
            appThemePalette = AppThemePalette.PlayfulMentor,
            plusMode = false,
            developerMode = false,
            hasSeenOnboarding = false,
            practiceRoundMultiplier = 1f,
        )
    }
}
