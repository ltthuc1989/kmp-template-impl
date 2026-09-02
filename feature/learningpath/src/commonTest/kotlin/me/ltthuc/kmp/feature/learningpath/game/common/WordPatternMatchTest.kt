package me.ltthuc.kmp.feature.learningpath.game.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden test cho [wordHasPattern] — luật nhận ra vần BAO của một unit cấp 3.
 *
 * Bubble Pop dùng nó để loại `a_e` (unit 1) và `i_e` (unit 2) khỏi danh sách vòng chơi:
 * hai vần đó khớp CẢ 12 từ của unit nên là cách viết chung, không phải vần luyện riêng.
 *
 * Bảng chép tay từ `curriculum.json`, KHÔNG sinh từ chính hàm đang test. Sai luật này là
 * bé được khen khi bấm nhầm, hoặc bấm đúng mà không được tính — không lỗi nào nổ ra.
 */
class WordPatternMatchTest {

    /** 12 từ mỗi unit, đúng thứ tự `curriculum.json`. */
    private val unitWords = mapOf(
        "U1" to listOf("tape", "cape", "cane", "mane", "game", "cake", "name", "lake", "gate", "wave", "skate", "cave"),
        "U2" to listOf("kite", "pine", "ripe", "fine", "lime", "bike", "time", "hike", "five", "nine", "dive", "line"),
        "U3" to listOf("home", "bone", "cone", "rope", "cube", "mute", "cute", "mule", "tube", "June", "tune", "rule"),
        "U4" to listOf("rain", "nail", "tail", "wait", "bay", "day", "say", "pay", "sail", "mail", "hay", "May"),
        "U5" to listOf("bee", "feet", "seed", "jeep", "leaf", "eat", "sea", "meat", "candy", "key", "happy", "money"),
        "U6" to listOf("light", "night", "high", "right", "pie", "tie", "lie", "die", "spy", "sky", "cry", "my"),
        "U7" to listOf("boat", "coat", "soap", "road", "bow", "row", "yellow", "pillow", "goat", "toad", "elbow", "window"),
        "U8" to listOf("blue", "glue", "clue", "Tuesday", "fruit", "suit", "new", "dew", "moon", "zoo", "food", "boot"),
    )

    /** vần → đúng những từ trong unit phải khớp. 27 vần của cấp 3 (25 thành vòng chơi). */
    private val golden = listOf(
        Triple("U1", "a_e", unitWords.getValue("U1")), // vần bao của cả unit
        Triple("U1", "ame", listOf("game", "name")),
        Triple("U1", "ake", listOf("cake", "lake")),
        Triple("U1", "ate", listOf("gate", "skate")),
        Triple("U1", "ave", listOf("wave", "cave")),
        Triple("U2", "i_e", unitWords.getValue("U2")),
        Triple("U2", "ime", listOf("lime", "time")),
        Triple("U2", "ike", listOf("bike", "hike")),
        Triple("U2", "ive", listOf("five", "dive")),
        Triple("U2", "ine", listOf("pine", "fine", "nine", "line")),
        Triple("U3", "o_e", listOf("home", "bone", "cone", "rope")),
        Triple("U3", "u_e", listOf("cube", "mute", "cute", "mule", "tube", "June", "tune", "rule")),
        Triple("U4", "ai", listOf("rain", "nail", "tail", "wait", "sail", "mail")),
        Triple("U4", "ay", listOf("bay", "day", "say", "pay", "hay", "May")),
        Triple("U5", "ee", listOf("bee", "feet", "seed", "jeep")),
        Triple("U5", "ea", listOf("leaf", "eat", "sea", "meat")),
        Triple("U5", "y", listOf("candy", "happy")),
        Triple("U5", "ey", listOf("key", "money")),
        Triple("U6", "igh", listOf("light", "night", "high", "right")),
        Triple("U6", "ie", listOf("pie", "tie", "lie", "die")),
        Triple("U6", "y", listOf("spy", "sky", "cry", "my")),
        Triple("U7", "oa", listOf("boat", "coat", "soap", "road", "goat", "toad")),
        Triple("U7", "ow", listOf("bow", "row", "yellow", "pillow", "elbow", "window")),
        Triple("U8", "ue", listOf("blue", "glue", "clue", "Tuesday")),
        Triple("U8", "ui", listOf("fruit", "suit")),
        Triple("U8", "ew", listOf("new", "dew")),
        Triple("U8", "oo", listOf("moon", "zoo", "food", "boot")),
    )

    @Test
    fun `27 vần chia từ đúng như bảng vàng`() {
        for ((unit, pattern, want) in golden) {
            val got = unitWords.getValue(unit).filter { wordHasPattern(it, pattern) }
            assertEquals(want, got, "$unit vần '$pattern'")
        }
    }

    @Test
    fun `không vần nào khớp rỗng`() {
        // Vòng rỗng = màn chơi không có bong bóng mục tiêu nào, hết 30s mà không bấm được gì.
        for ((unit, pattern, _) in golden) {
            val got = unitWords.getValue(unit).filter { wordHasPattern(it, pattern) }
            assertTrue(got.isNotEmpty(), "$unit vần '$pattern' không khớp từ nào")
        }
    }

    @Test
    fun `vần magic-e đi luật đuôi, vần nguyên âm đi luật nhãn`() {
        // `game` tách ra `g · a_e · m` nên nhãn nguyên âm là "a_e", không phải "ame".
        assertTrue(wordHasPattern("game", "ame"), "luật đuôi")
        assertTrue(wordHasPattern("game", "a_e"), "luật nhãn nguyên âm")
        // `light` kết thúc bằng "ght" — luật đuôi sai ở đây, phải đi luật nhãn.
        assertTrue(wordHasPattern("light", "igh"))
        assertTrue(!wordHasPattern("light", "ame"))
    }

    @Test
    fun `chữ y tách đúng hai âm ở hai unit`() {
        // U5 dạy /iː/ (candy, happy), U6 dạy /aɪ/ (spy, my) — cùng nhãn "y".
        assertTrue(wordHasPattern("candy", "y") && wordHasPattern("spy", "y"))
        // nhưng chúng không bao giờ đứng chung một unit, nên không lẫn nhau trong màn chơi
        assertTrue(unitWords.getValue("U5").none { it in unitWords.getValue("U6") })
    }

    @Test
    fun `đúng hai vần ôm trọn unit, và chúng bị loại khỏi vòng chơi`() {
        // `BubblePopViewModel.dropUmbrellaPatterns` loại vần nào khớp CẢ 12 từ của unit:
        // nó là cách viết chung của unit, không phải một vần được luyện riêng.
        // Khoá lại danh sách ở đây để cấp 4-5 thêm vần bao mới thì test kêu.
        val umbrella = golden
            .filter { (unit, pattern, _) ->
                unitWords.getValue(unit).all { wordHasPattern(it, pattern) }
            }
            .map { (unit, pattern, _) -> "$unit:$pattern" }
        assertEquals(listOf("U1:a_e", "U2:i_e"), umbrella)
    }

    @Test
    fun `đầu vào rỗng trả false chứ không nổ`() {
        assertTrue(!wordHasPattern("", "ame"))
        assertTrue(!wordHasPattern("game", ""))
        assertTrue(!wordHasPattern("   ", "  "))
    }
}
