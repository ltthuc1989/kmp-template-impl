package me.ltthuc.kmp.core.model

/** Một unit cùng các lesson của nó, dùng khi cần nhìn cả curriculum theo thứ tự học. */
data class UnitLessons(
    val unit: PhonicsUnit,
    val lessons: List<PhonicsLesson>,
)
