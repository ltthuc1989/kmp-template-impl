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
    /**
     * Cách tách từ cho bước 0 cấp 4+ (`black` → `bl`·`ack`), null khi dữ liệu không có.
     *
     * Đến từ cột `split1..4` của `phonics.csv` qua `curriculum.json`, là NGUỒN DUY NHẤT cho
     * cả hai phía: opw sinh mảnh tiếng theo đúng bảng này, app phóng chữ theo đúng bảng này.
     * Không suy bằng thuật toán trong app — cấp 4 có pattern ở đầu, giữa và cuối từ, có
     * `sch`, có `ce/ge/se` mang e câm, có từ hai chữ; một bảng chép tay do người duyệt
     * còn hơn hai thuật toán ở hai repo rình rập lệch nhau.
     */
    val blendSplit: BlendSplit? = null,
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

/**
 * Bảng tách một từ cho bước 0: [chunks] ghép lại (bỏ dấu cách) bằng đúng từ, và mảnh ở
 * [patternIndex] là mảnh mang pattern bài dạy (`ack` của `black` là mảnh 1, pattern là mảnh 0).
 *
 * Mảnh mang pattern có thể DÀI HƠN pattern: `rice` tách `ri`·`ce` với pattern `c` — chữ `e`
 * câm đi cùng mảnh để đọc thành một tiếng, nhưng chỉ `c` được tô màu pattern.
 */
data class BlendSplit(
    val chunks: List<String>,
    val patternIndex: Int,
) {
    val patternChunk: String get() = chunks.getOrElse(patternIndex) { "" }
}
