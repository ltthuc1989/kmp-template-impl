package me.ltthuc.kmp.core.model

data class UnitCard(
    val unit: PhonicsUnit,
    val status: UnitStatus,
    val completionCount: Int,
    val previewLetters: List<UnitLetterPreview>,
)

data class UnitLetterPreview(
    val letter: String,
    val emoji: String?,
)

enum class UnitStatus {
    Completed,
    Active,
    Unlocked,

    /** Locked because the previous unit in the level has not been completed yet (sequential gate). */
    Locked,

    /** Locked because the level is paid and the user has not purchased it (paywall gate). */
    PremiumLocked,
}
