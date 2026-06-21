package me.ltthuc.kmp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class AppSetting(
    val id: String,
    val theme: Theme,
    // UI language. System = follow device locale; English/Vietnamese force a specific UI language.
    val language: Language = Language.System,
    val appThemePalette: AppThemePalette,
    val plusMode: Boolean,
    val developerMode: Boolean,
    // Levels the user actually owns via purchase (per-level $5 or full bundle $20).
    // Synced from RevenueCat entitlements; persisted for offline access.
    val ownedLevelIds: Set<String> = emptySet(),
    // Levels a parent has chosen to open fully (skip the sequential gate). Only takes effect
    // on levels that are also owned — it NEVER bypasses the paywall.
    val manualUnlockedLevelIds: Set<String> = emptySet(),
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
    // Restore-on-relaunch: the last screen the user was on (+ its level/unit context).
    val lastScreen: LastScreen = LastScreen.NONE,
    val lastLevelId: String = "",
    val lastUnitId: String = "",
) {
    val hasPrivilege get() = plusMode || developerMode

    /** True if the user may access the paid units of [levelId] (purchased the level, owns the
     * bundle → all levels in [ownedLevelIds], or is in developer mode). Note: `plusMode` is NOT
     * consulted here — owning one level must not unlock the others. */
    fun isLevelOwned(levelId: String): Boolean =
        developerMode || levelId in ownedLevelIds

    /** True if every unit of [levelId] should open at once (sequential gate removed). Only an
     * OWNED level can be opened fully — this never unlocks a level the user has not paid for. */
    fun isLevelOpenedFully(levelId: String): Boolean =
        developerMode || (isLevelOwned(levelId) && levelId in manualUnlockedLevelIds)

    /** Which screen the user was last on — used to restore the right backstack on app relaunch. */
    enum class LastScreen { NONE, LEVEL_LIST, UNIT_LIST, LESSON_MAP }

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
