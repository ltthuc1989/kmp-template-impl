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
 * Từ [FIRST_LESSON_KEY_LEVEL] khoá đổi hẳn sang theo BÀI — xem [rimeAudioKeyFor].
 *
 * Nhãn trùng giữa hai bài của cùng unit thì bài SAU thắng: `ear` của L5U4 là bài
 * `ear_eer` (/ɪr/ — ear, clear) chứ không phải `ea_ear` (/ɛr/ — bear), user chốt
 * 2026-10-03. Bubble Pop và Memory Match chỉ có MỘT vòng/một thẻ cho mỗi nhãn nên phải
 * chọn một âm; Fill Letter thì đọc theo đúng bài của từ đang chơi nên không vướng.
 *
 * Cùng công thức phía sinh audio: `opw_audio_project/scripts/prompts.py:rime_audio_key()`.
 * Sửa bên nào thì sửa cả bên kia rồi chạy golden test hai phía.
 */
internal fun List<PhonicsLesson>.rimeAudioKeys(): Map<String, String> = buildMap {
    for (lesson in this@rimeAudioKeys) {
        if ((lesson.level() ?: 1) < FIRST_SPLIT_KEY_LEVEL) continue
        for (pattern in lesson.lessonPatterns()) {
            put(pattern, lesson.rimeAudioKeyFor(pattern))
        }
    }
}

/**
 * Khoá file của nhãn [pattern] trong bài này.
 *
 * Cấp 3-4: [rimeAudioKey] — tên nhãn, nhãn một ký tự gắn thêm âm.
 *
 * Cấp 5+: `<mã bài>_<nhãn>` (`l5u1_er_or_or`). Luật theo nhãn hết đường dùng ở cấp 5:
 * `ow` `oo` `ea` `st` đã là file cấp 3/4 đọc âm KHÁC (cow /aʊ/ ≠ bow /oʊ/), ghi vào là đè
 * bản đã ship; trong cấp 5 `or` (doctor /ɚ/ ≠ horse /ɔr/) và `ear` (bear ≠ ear) cũng hai
 * âm một tên; còn `soundSpelling` của hậu tố hai chữ đẻ khoá vô nghĩa `e_eee-eye`. Gắn mã
 * bài thì không trùng được nữa — giá phải trả là hai bài cùng âm (`o` của hai bài schwa)
 * mang hai file giống hệt, vài KB.
 */
internal fun PhonicsLesson.rimeAudioKeyFor(pattern: String): String =
    if ((level() ?: 1) >= FIRST_LESSON_KEY_LEVEL) {
        lessonRimeAudioKey(id, pattern)
    } else {
        rimeAudioKey(pattern, soundSpelling)
    }

/** Nhân của [rimeAudioKeyFor] cho cấp 5+, tách riêng để test không cần dựng [PhonicsLesson]. */
internal fun lessonRimeAudioKey(lessonId: String, pattern: String): String =
    "${lessonId.trim().lowercase()}_${pattern.trim().lowercase()}"

/** Nhân của [rimeAudioKeys], tách riêng để test được mà không cần dựng [PhonicsLesson]. */
internal fun rimeAudioKey(pattern: String, soundSpelling: String): String {
    val label = pattern.trim().lowercase()
    if (label.length != 1) return label
    val sound = soundSpelling.trim().lowercase()
    return if (sound.isEmpty()) label else "${label}_$sound"
}

/** Cấp đầu tiên có nhãn vần đụng nhau giữa các unit, nên phải gắn thêm âm vào khoá. */
private const val FIRST_SPLIT_KEY_LEVEL = 3

/** Cấp đầu tiên khoá theo mã bài thay vì theo nhãn — xem [rimeAudioKeyFor]. */
private const val FIRST_LESSON_KEY_LEVEL = 5
