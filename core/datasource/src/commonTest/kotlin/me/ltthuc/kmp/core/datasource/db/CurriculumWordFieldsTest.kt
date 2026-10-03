package me.ltthuc.kmp.core.datasource.db

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Khoá đường đi của dữ liệu TỪ trong `curriculum.json` xuống `wordsJson` của Room.
 *
 * Vì sao cần: chuỗi này có BA tầng khai kiểu riêng — [LessonWordDto] (đọc JSON),
 * [NormalizedLessonWord] (ghi vào `wordsJson`) và `LessonWordJson` bên `core:repository`
 * (đọc ra). Thêm một trường mà quên tầng giữa thì **không lỗi gì cả**: JSON vẫn parse
 * (`ignoreUnknownKeys`), app vẫn chạy, trường chỉ lặng lẽ biến mất.
 *
 * Đã dính đúng lỗi đó ngày 2026-09-24 với `silent` (chữ câm cấp 5): thêm ở `curriculum.json`
 * và ở model, quên ở đây → 12 từ unit 7 vẽ chữ câm đậm như chữ thường, không có dấu hiệu nào.
 */
class CurriculumWordFieldsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `mọi trường của một từ đi hết ba tầng, không rơi ở khâu chuẩn hoá`() {
        val raw = """
            {
              "word": "knife",
              "displays": [{"type": "image", "path": "files/images/vocab/knife.webp"},
                           {"type": "emoji", "char": "🔪"}],
              "split": ["kn", "ife"],
              "patternIndex": 0,
              "silent": [0]
            }
        """.trimIndent()

        val dto = json.decodeFromString<LessonWordDto>(raw)
        assertEquals(listOf("kn", "ife"), dto.split)
        assertEquals(0, dto.patternIndex)
        assertEquals(listOf(0), dto.silent)

        // Chuỗi ký tự `wordsJson` thật sự nằm trong Room — đây là chỗ trường hay bị rơi.
        val stored = json.encodeToString(listOf(dto).map { it.normalize() })
        assertTrue(stored.contains("\"split\""), "wordsJson phải mang split")
        assertTrue(stored.contains("\"patternIndex\""), "wordsJson phải mang patternIndex")
        assertTrue(stored.contains("\"silent\""), "wordsJson phải mang silent (chữ câm cấp 5)")

        val back = json.decodeFromString<List<NormalizedLessonWord>>(stored).single()
        assertEquals(listOf("kn", "ife"), back.split)
        assertEquals(0, back.patternIndex)
        assertEquals(listOf(0), back.silent)
        assertEquals(2, back.displays.size, "ảnh đứng trước emoji")
    }

    @Test
    fun `từ không khai chữ câm thì trường rỗng, không phải null`() {
        val dto = json.decodeFromString<LessonWordDto>("""{"word":"car","emoji":"🚗"}""")
        val back = dto.normalize()
        assertEquals(emptyList(), back.silent)
        assertEquals(emptyList(), back.split)
        assertEquals(-1, back.patternIndex)
        // `emoji` cũ được nâng thành `displays` để phía đọc chỉ có một nguồn.
        assertEquals(1, back.displays.size)
    }
}
