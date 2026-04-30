package me.ltthuc.kmp.core.model

data class PhonicsLesson(
    val id: String,
    val unitId: String,
    val letter: String,
    val displayLetter: String,
    val soundSpelling: String,
    val sentence: String,
    val stretchedWord: String,
    val orderIndex: Int,
    val words: List<LessonWord>,
    val chantTexts: List<String>,
    val chantOrder: List<Int>,
)

data class LessonWord(
    val word: String,
    val emoji: String?,
) {
    val text: String get() = word
    val imageAsset: String? get() = null
}
