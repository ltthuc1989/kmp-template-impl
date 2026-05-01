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
