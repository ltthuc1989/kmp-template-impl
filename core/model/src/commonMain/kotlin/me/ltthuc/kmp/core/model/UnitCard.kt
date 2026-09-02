package me.ltthuc.kmp.core.model

data class UnitCard(
    val unit: PhonicsUnit,
    val status: UnitStatus,
    val completionCount: Int,
    val previewLetters: List<UnitLetterPreview>,
)

data class UnitLetterPreview(
    val letter: String,
    /**
     * Từ đại diện của bài, để màn chọn unit vẽ hình xem trước.
     *
     * Mang cả [LessonWord] chứ không mang sẵn chuỗi emoji: 35 từ trên cả 5 cấp có ảnh
     * WebP vẽ riêng vì emoji của chúng gây hiểu nhầm (`cave` → 🕳️ cái hố, `nail` → 🔨
     * cái búa). Chốt emoji ở tầng model thì tầng vẽ không còn đường lấy ảnh.
     */
    val word: LessonWord?,
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
