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
     *
     * Phải LỌC theo kiểu chứ không lấy phần tử đầu rồi ép kiểu: từ nào có ảnh riêng thì
     * ảnh đứng ĐẦU `displays` (ảnh được ưu tiên hơn emoji), nên phép ép kiểu trả null
     * cho đúng những từ đã bỏ công vẽ ảnh. Hậu quả im lặng: 4 game lọc bằng
     * `!it.emoji.isNullOrBlank()` (Drag Words, Pick Word, Fill Letter, Memory Match) tự
     * loại sạch nhóm từ đó, và thẻ bài trong Lesson Map rơi về "📘".
     */
    val emoji: String?
        get() = displays.filterIsInstance<WordDisplay.Emoji>().firstOrNull()?.char
}
