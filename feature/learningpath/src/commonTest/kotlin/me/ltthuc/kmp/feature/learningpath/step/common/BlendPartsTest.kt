package me.ltthuc.kmp.feature.learningpath.step.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Kiểm tra [blendParts] — phép tách từ mà màn ghép vần cấp 3 dựa vào để biết đọc mảnh nào
 * và phóng to ký tự nào.
 *
 * Bài quan trọng nhất là [mọi từ đều phủ kín, không chồng lấn]: nếu một ký tự không nằm
 * trong mảnh nào thì nó sẽ không bao giờ được phóng to, còn nếu nằm trong hai mảnh thì nó
 * nhấp nháy hai lần. Cả hai đều là lỗi ÂM THẦM — màn hình vẫn chạy, chỉ là sai.
 */
class BlendPartsTest {

    /** 96 từ của cấp 3, chép từ `data/level_3/phonics.csv`. */
    private val allWords = listOf(
        "tape", "cape", "cane", "mane", "game", "cake", "name", "lake",
        "gate", "wave", "skate", "cave", "kite", "pine", "ripe", "fine",
        "lime", "bike", "time", "hike", "five", "nine", "dive", "line",
        "home", "bone", "cone", "rope", "cube", "mute", "cute", "mule",
        "tube", "June", "tune", "rule", "rain", "nail", "tail", "wait",
        "bay", "day", "say", "pay", "sail", "mail", "hay", "May",
        "bee", "feet", "seed", "jeep", "leaf", "eat", "sea", "meat",
        "candy", "key", "happy", "money", "light", "night", "high", "right",
        "pie", "tie", "lie", "die", "spy", "sky", "cry", "my",
        "boat", "coat", "soap", "road", "bow", "row", "yellow", "pillow",
        "goat", "toad", "elbow", "window", "blue", "glue", "clue", "Tuesday",
        "fruit", "suit", "new", "dew", "moon", "zoo", "food", "boot",
    )

    private fun labels(word: String) = blendParts(word).map { it.label }

    @Test
    fun `magic-e tách thành onset + nguyên âm rời + phụ âm giữa`() {
        assertEquals(listOf("t", "a_e", "p"), labels("tape"))
        assertEquals(listOf("c", "a_e", "n"), labels("cane"))
        assertEquals(listOf("g", "a_e", "m"), labels("game"), "vần bài là 'ame' nhưng từ vẫn tách magic-e")
        assertEquals(listOf("h", "o_e", "m"), labels("home"))
        assertEquals(listOf("c", "u_e", "b"), labels("cube"))
        assertEquals(listOf("f", "i_e", "v"), labels("five"))
    }

    @Test
    fun `nguyên âm magic-e phủ đúng hai ký tự rời nhau`() {
        val vowel = blendParts("tape").single { it.kind == BlendPieceKind.Vowel }
        assertEquals(listOf(1..1, 3..3), vowel.spans, "phải là 'a' và 'e' cuối, bỏ qua 'p'")
    }

    @Test
    fun `cụm phụ âm đầu giữ nguyên khối`() {
        assertEquals(listOf("sk", "a_e", "t"), labels("skate"))
        assertEquals(listOf("sp", "y"), labels("spy"))
        assertEquals(listOf("cr", "y"), labels("cry"))
        assertEquals(listOf("bl", "ue"), labels("blue"))
        assertEquals(listOf("fr", "ui", "t"), labels("fruit"))
    }

    @Test
    fun `tổ hợp nguyên âm tách đúng`() {
        assertEquals(listOf("r", "ai", "n"), labels("rain"))
        assertEquals(listOf("b", "ay"), labels("bay"), "không có phần đuôi")
        assertEquals(listOf("b", "ee"), labels("bee"))
        assertEquals(listOf("f", "ee", "t"), labels("feet"))
        assertEquals(listOf("l", "igh", "t"), labels("light"), "igh là một khối, không tách i + gh")
        assertEquals(listOf("h", "igh"), labels("high"))
        assertEquals(listOf("b", "oa", "t"), labels("boat"))
        assertEquals(listOf("m", "oo", "n"), labels("moon"))
        assertEquals(listOf("p", "ie"), labels("pie"))
        assertEquals(listOf("m", "y"), labels("my"))
    }

    @Test
    fun `từ không có phụ âm đầu thì không sinh mảnh onset`() {
        assertEquals(listOf("ea", "t"), labels("eat"))
        assertEquals(listOf(BlendPieceKind.Vowel, BlendPieceKind.Coda), blendParts("eat").map { it.kind })
    }

    @Test
    fun `tám từ hai âm tiết đánh vần từng chữ, chữ đôi đọc một lần`() {
        assertEquals(listOf("c", "a", "n", "d", "y"), labels("candy"))
        assertEquals(listOf("h", "a", "p", "y"), labels("happy"), "hai chữ p đọc một lần")
        assertEquals(listOf("m", "o", "n", "ey"), labels("money"))
        assertEquals(listOf("y", "e", "l", "ow"), labels("yellow"), "hai chữ l đọc một lần")
        assertEquals(listOf("p", "i", "l", "ow"), labels("pillow"))
        assertEquals(listOf("e", "l", "b", "ow"), labels("elbow"))
        assertEquals(listOf("w", "i", "n", "d", "ow"), labels("window"))
        assertEquals(listOf("t", "ue", "s", "d", "ay"), labels("Tuesday"), "có tới hai tổ hợp nguyên âm")
    }

    @Test
    fun `chữ đôi vẫn phủ cả hai ký tự dù chỉ đọc một tiếng`() {
        val p = blendParts("happy").single { it.label == "p" }
        assertEquals(listOf(2..3), p.spans, "phải trùm cả 'pp', nếu không chữ p thứ hai không sáng lên")
    }

    @Test
    fun `mọi từ đều phủ kín, không chồng lấn`() {
        for (word in allWords) {
            val pieces = blendParts(word)
            assertTrue(pieces.isNotEmpty(), "$word: không tách được mảnh nào")
            val covered = pieces.flatMap { piece -> piece.spans.flatMap { it.toList() } }.sorted()
            assertEquals(
                word.indices.toList(),
                covered,
                "$word tách thành ${pieces.map { it.label }} — phải phủ đúng mỗi ký tự một lần",
            )
        }
    }

    @Test
    fun `mọi từ đều có đúng một nguyên âm chính`() {
        for (word in allWords) {
            val vowels = blendParts(word).count { it.kind == BlendPieceKind.Vowel }
            val expected = if (word.lowercase() in TWO_SYLLABLE_WORDS) 2 else 1
            assertEquals(expected, vowels, "$word có $vowels mảnh nguyên âm")
        }
    }

    /** Từ hai âm tiết có hai mảnh nguyên âm: nguyên âm âm tiết đầu + vần cuối. */
    private val TWO_SYLLABLE_WORDS = setOf(
        "candy",
        "happy",
        "money",
        "yellow",
        "pillow",
        "elbow",
        "window",
        "tuesday",
    )
}
