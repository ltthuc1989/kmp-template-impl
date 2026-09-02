package me.ltthuc.kmp.feature.learningpath.game.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden test cho [rimeAudioKey] — khoá tên file trong `rimes/` và `find_rime/`.
 *
 * Bảng chép tay từ `opw_audio_project/data/level_3/phonics.csv` (cột `letter` +
 * `sound_spelling`), KHÔNG sinh từ chính hàm đang test.
 *
 * Bản song sinh phía Python: `opw_audio_project/tests/test_rime_audio_key.py`.
 * Sửa một bên thì chạy cả hai — lệch nhau là app trỏ vào file mà bên sinh không đặt tên
 * như vậy, và bé bấm bong bóng thì im lặng.
 */
class RimeAudioKeyTest {

    @Test
    fun `nhãn từ hai ký tự trở lên giữ nguyên`() {
        val golden = listOf(
            "a_e", "ame", "ake", "ate", "ave",
            "i_e", "ime", "ike", "ive", "ine",
            "o_e", "u_e",
            "ai", "ay",
            "ee", "ea", "ey",
            "igh", "ie",
            "oa", "ow",
            "ue", "ui", "ew", "oo",
        )
        for (label in golden) {
            assertEquals(label, rimeAudioKey(label, "aaay"), "nhãn '$label'")
        }
    }

    @Test
    fun `chữ y tách theo âm của bài`() {
        // L3U5_y_ey dạy /iː/ (candy, happy); L3U6_y dạy /aɪ/ (spy, my). Cùng nhãn "y".
        assertEquals("y_eee", rimeAudioKey("y", "eee"))
        assertEquals("y_eye", rimeAudioKey("y", "eye"))
    }

    @Test
    fun `khoá luôn viết thường và bỏ khoảng trắng thừa`() {
        assertEquals("ame", rimeAudioKey(" AME ", "aaay"))
        assertEquals("y_eee", rimeAudioKey("Y", " EEE "))
    }

    @Test
    fun `thiếu soundSpelling thì giữ nhãn trần thay vì đẻ ra khoá cụt`() {
        assertEquals("y", rimeAudioKey("y", ""))
        assertEquals("y", rimeAudioKey("y", "   "))
    }
}
