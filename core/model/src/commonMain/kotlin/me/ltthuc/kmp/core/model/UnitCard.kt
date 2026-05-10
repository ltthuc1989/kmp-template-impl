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
    Locked,
}
