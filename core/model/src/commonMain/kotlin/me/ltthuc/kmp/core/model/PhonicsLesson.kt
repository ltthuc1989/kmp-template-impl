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
    val displays: List<WordDisplay>,
) {
    val text: String get() = word

    /**
     * First emoji variant for legacy preview/hero use cases (deterministic, not random).
     * Returns null when no [WordDisplay.Emoji] variant exists in [displays].
     */
    val emoji: String?
        get() = (displays.firstOrNull() as? WordDisplay.Emoji)?.char
}
