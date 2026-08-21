package me.ltthuc.kmp.feature.learningpath.step.common

import me.ltthuc.kmp.core.model.PhonicsLesson

/**
 * Cấp độ của lesson, suy từ id ("L2U1_a" → 2). Trả null nếu id không đúng khuôn.
 *
 * Không đọc từ trường riêng vì `PhonicsLesson` không mang cấp độ — id là chỗ duy
 * nhất có thông tin đó, và curriculum.json luôn đặt id theo khuôn `L{n}U{n}_`.
 */
internal fun PhonicsLesson.level(): Int? =
    LESSON_ID_LEVEL_REGEX.find(id)?.groupValues?.get(1)?.toIntOrNull()

/**
 * Vần/pattern của lesson, suy từ mã `letter` trong CSV.
 *
 * Hai luật khác nhau, vì hai cấp độ đặt mã khác nhau:
 *
 *   Cấp 1-2   tiền tố HAI đoạn "SHORT-{nguyên âm}", vần tính từ đoạn thứ 3
 *       "SHORT-A"        → []             (lesson nguyên âm đơn, không dạy vần)
 *       "SHORT-A-AM"     → ["am"]
 *       "SHORT-A-AD-AG"  → ["ad", "ag"]
 *
 *   Cấp 3+    tiền tố MỘT đoạn, và chính nó cũng là một pattern
 *       "A_E"            → ["a_e"]
 *       "A_E-AME-AKE"    → ["ame", "ake"]  (bỏ tiền tố magic-e, giữ vần thật)
 *       "U_E-1"          → ["u_e"]         (đuôi số chỉ để tách 2 lesson trùng vần)
 *       "AI-AY"          → ["ai", "ay"]
 *       "IGH"            → ["igh"]
 *
 * Với cấp 3, kết quả LUÔN bằng `displayLetter` tách theo dấu cách. Đó là bất biến
 * mà golden test khoá lại: chữ hiện trên màn hình và vần dùng để suy tên file audio
 * phải là một, lệch nhau thì bé nghe một đằng nhìn một nẻo mà không có lỗi nào nổ ra.
 *
 * Cấp 2 KHÔNG có bất biến đó — lesson nguyên âm đơn phải trả rỗng để nhánh "không
 * dạy vần" nhận ra. Đừng gộp hai luật làm một.
 *
 * Cùng công thức `opw_audio_project/scripts/prompts.py:parse_patterns()`. Sửa bên
 * nào thì sửa cả bên kia, rồi chạy golden test cả hai phía.
 */
internal fun PhonicsLesson.lessonPatterns(): List<String> =
    parsePatterns(letter, level() ?: FIRST_PATTERN_LEVEL)

/** Nhân của [lessonPatterns], tách riêng để test được mà không cần dựng `PhonicsLesson`. */
internal fun parsePatterns(letter: String, level: Int): List<String> {
    val segs = letter.trim().lowercase().split('-').filter { it.isNotEmpty() }
    if (segs.isEmpty()) return emptyList()
    if (level <= FIRST_PATTERN_LEVEL) return segs.drop(2)
    if (segs[0].contains('_')) {
        // "a_e" là cách VIẾT vần chứ không phải vần đọc lên được, nên bỏ đi khi
        // lesson có vần thật đi kèm. Đoạn toàn số ("u_e-1") chỉ để tách lesson.
        val rest = segs.drop(1).filterNot { seg -> seg.all { it.isDigit() } }
        return rest.ifEmpty { listOf(segs[0]) }
    }
    return segs
}

private val LESSON_ID_LEVEL_REGEX = Regex("""^L(\d+)U\d+_""")

/** Cấp cuối cùng còn dùng khuôn mã "SHORT-{nguyên âm}-{vần}". Từ cấp sau là khuôn mới. */
private const val FIRST_PATTERN_LEVEL = 2
