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
    /**
     * Chỉ số những ký tự CÂM trong [word] (`knife` → {0}, `lamb` → {3}), rỗng khi từ không có.
     *
     * Chỉ cấp 5 unit 7 dùng: sách OPW5 in chữ câm màu hồng NHẠT ngay trên thẻ pattern
     * (`k n`, `w r`, `m b`, `s t`, và `e` của `glove`) — đó chính là bài học, không phải
     * trang trí. Cũng đến từ `phonics.csv` (cột `silent`) qua `curriculum.json`, cùng lý do
     * với [blendSplit]: một bảng người duyệt, không suy bằng thuật toán.
     *
     * Để theo TỪNG TỪ chứ không theo lesson vì màn hình tô theo ký tự, và vì cùng một chữ
     * có thể xuất hiện hai lần trong từ mà chỉ một chỗ câm (`rhubarb` có hai `b`).
     */
    val silentIndices: Set<Int> = emptySet(),
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
