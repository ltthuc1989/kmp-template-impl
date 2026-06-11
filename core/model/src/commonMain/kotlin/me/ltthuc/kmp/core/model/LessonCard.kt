package me.ltthuc.kmp.core.model

data class LessonCard(
    val lesson: PhonicsLesson,
    val status: LessonStatus,
    val completionCount: Int,
)

enum class LessonStatus {
    Completed,
    Active,
    Unlocked,
    Locked,
}
