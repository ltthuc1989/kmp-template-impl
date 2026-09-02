package me.ltthuc.kmp.feature.learningpath.game.common

import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.feature.learningpath.step.common.lessonPatterns
import me.ltthuc.kmp.feature.learningpath.step.common.level

/**
 * Nhãn vần hiện trên bong bóng / mặt thẻ → khoá file audio của trò chơi
 * (`rimes/<khoá>.mp3` cho tiếng vần, `find_rime/<khoá>.mp3` cho câu gọi đầu vòng).
 *
 * Hai thư mục đó KHÔNG chia theo cấp độ — mọi cấp đổ chung vào một chỗ. Nhãn nào đọc
 * lên khác nhau ở hai bài khác nhau mà mang cùng tên file thì bài sau đè bài trước, và
 * bé nghe sai âm không có lỗi nào nổ ra.
 *
 * Cấp 3 vướng đúng chỗ đó: chữ `y` dạy HAI âm — /iː/ ở `L3U5_y_ey` (candy, happy) và
 * /aɪ/ ở `L3U6_y` (spy, my). Chưa hết, nhãn một ký tự còn đụng luôn `phonemes/y.mp3` —
 * âm /j/ của chữ cái y học ở cấp 1 — nên để nguyên là sai theo kiểu thứ hai.
 *
 * Luật: **nhãn dài đúng một ký tự thì gắn thêm `soundSpelling`** (`y` + `eee` → `y_eee`,
 * `y` + `eye` → `y_eye`). Nhãn từ hai ký tự trở lên tự nó đã phân biệt được nên giữ
 * nguyên. Luật đọc theo ĐỘ DÀI NHÃN chứ không liệt kê "chữ y", vì cấp sau còn nhãn một
 * ký tự khác (`e` của `-le`, `o` của `-tion`) và mỗi lần liệt kê tay là một lần quên.
 *
 * Chỉ áp từ [FIRST_SPLIT_KEY_LEVEL]. Cấp 1-2 đã ship bộ file theo tên nhãn trần, và ở
 * đó nhãn một ký tự là NGUYÊN ÂM ĐƠN ("a" của `L2U1_a`) — `phonemes/a.mp3` chính là âm
 * cần phát, không có gì để tách.
 *
 * Cùng công thức phía sinh audio: `opw_audio_project/scripts/prompts.py:rime_audio_key()`.
 * Sửa bên nào thì sửa cả bên kia rồi chạy golden test hai phía.
 */
internal fun List<PhonicsLesson>.rimeAudioKeys(): Map<String, String> = buildMap {
    for (lesson in this@rimeAudioKeys) {
        if ((lesson.level() ?: 1) < FIRST_SPLIT_KEY_LEVEL) continue
        for (pattern in lesson.lessonPatterns()) {
            put(pattern, rimeAudioKey(pattern, lesson.soundSpelling))
        }
    }
}

/** Nhân của [rimeAudioKeys], tách riêng để test được mà không cần dựng [PhonicsLesson]. */
internal fun rimeAudioKey(pattern: String, soundSpelling: String): String {
    val label = pattern.trim().lowercase()
    if (label.length != 1) return label
    val sound = soundSpelling.trim().lowercase()
    return if (sound.isEmpty()) label else "${label}_$sound"
}

/** Cấp đầu tiên có nhãn vần đụng nhau giữa các unit, nên phải gắn thêm âm vào khoá. */
private const val FIRST_SPLIT_KEY_LEVEL = 3
