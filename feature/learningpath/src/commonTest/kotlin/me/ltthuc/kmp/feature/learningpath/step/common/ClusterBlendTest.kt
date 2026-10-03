package me.ltthuc.kmp.feature.learningpath.step.common

import me.ltthuc.kmp.core.model.BlendSplit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Golden test cho bước 0 cấp 4.
 *
 * Bảng dưới chép tay từ `opw_audio_project/data/level_4/phonics.csv` (cột `letter` và
 * `split1..4`) và từ mục lục sách OPW4 — KHÔNG sinh từ chính hàm đang test.
 *
 * Hai bất biến được khoá ở đây:
 *   1. cụm nào đọc thành một âm (dòng 1 không có phép cộng) — phải khớp phía audio
 *   2. ký tự nào của từ được tô hồng — lỗi #4 của bản soát 2026-08-31 tô nhầm nguyên âm
 */
class ClusterBlendTest {

    /** 40 cụm của 24 bài, kèm kiểu panel sách in. */
    private val goldenKinds: List<Pair<String, ClusterKind>> = listOf(
        "bl" to ClusterKind.Addition, "cl" to ClusterKind.Addition,
        "br" to ClusterKind.Addition, "cr" to ClusterKind.Addition,
        "fl" to ClusterKind.Addition, "gl" to ClusterKind.Addition,
        "fr" to ClusterKind.Addition, "gr" to ClusterKind.Addition,
        "pl" to ClusterKind.Addition, "sl" to ClusterKind.Addition,
        "dr" to ClusterKind.Addition, "tr" to ClusterKind.Addition,
        "sm" to ClusterKind.Addition, "sn" to ClusterKind.Addition,
        "sp" to ClusterKind.Addition, "sw" to ClusterKind.Addition,
        "st" to ClusterKind.Addition,
        "sh" to ClusterKind.Single,
        "ch" to ClusterKind.Single, "tch" to ClusterKind.Single,
        "ph" to ClusterKind.Single, "wh" to ClusterKind.Single,
        "th" to ClusterKind.Single,
        "ck" to ClusterKind.Single, "qu" to ClusterKind.Single,
        "ng" to ClusterKind.Single, "nk" to ClusterKind.Single,
        // Cụm CUỐI từ vẫn là phép cộng: hai âm rời (chốt câu 1).
        "nd" to ClusterKind.Addition, "nt" to ClusterKind.Addition,
        "lt" to ClusterKind.Addition, "mp" to ClusterKind.Addition,
        "sk" to ClusterKind.Addition, "sc" to ClusterKind.Addition,
        "spr" to ClusterKind.Addition, "str" to ClusterKind.Addition,
        "spl" to ClusterKind.Addition, "squ" to ClusterKind.Addition,
        "c" to ClusterKind.Single, "g" to ClusterKind.Single, "s" to ClusterKind.Single,
    )

    @Test
    fun `40 cụm của cấp 4 chia đúng hai kiểu panel`() {
        assertEquals(40, goldenKinds.size, "24 bài cấp 4 có đúng 40 cụm")
        assertEquals(13, goldenKinds.count { it.second == ClusterKind.Single }, "10 bài kiểu B, gồm 13 cụm")
        for ((pattern, want) in goldenKinds) {
            assertEquals(want, clusterKind(pattern), "pattern=$pattern")
        }
    }

    @Test
    fun `toán hạng dòng 1 - qu giữ nguyên khối, bài một âm không có phép cộng`() {
        assertEquals(listOf("b", "l"), equationOperands("bl"))
        assertEquals(listOf("s", "p", "r"), equationOperands("spr"))
        assertEquals(listOf("s", "p", "l"), equationOperands("spl"))
        assertEquals(listOf("s", "qu"), equationOperands("squ"))
        assertEquals(listOf("n", "d"), equationOperands("nd"))
        // Kiểu B: dòng 1 chỉ một thẻ, không tách chữ ra cộng.
        assertEquals(emptyList(), equationOperands("sh"))
        assertEquals(emptyList(), equationOperands("qu"))
        assertEquals(emptyList(), equationOperands("c"))
        assertEquals(emptyList(), equationOperands(""))
    }

    @Test
    fun `chọn cụm theo mảnh pattern, không theo vị trí trong từ`() {
        // Bài trộn đầu/cuối từ — chỗ `endsWith` của cấp 3 sai (lỗi #2, soát 2026-08-31).
        assertEquals("sh", patternForChunk(listOf("sh"), "sh"))
        assertEquals("sk", patternForChunk(listOf("sk", "sc"), "sk"))
        assertEquals("sc", patternForChunk(listOf("sk", "sc"), "sc"))
        // Cụm dài thắng: `tch` cũng chứa `ch`.
        assertEquals("tch", patternForChunk(listOf("ch", "tch"), "tch"))
        assertEquals("ch", patternForChunk(listOf("ch", "tch"), "ch"))
        // Mảnh mang `e` câm vẫn tra ra cụm một chữ.
        assertEquals("c", patternForChunk(listOf("c"), "ce"))
        assertEquals("g", patternForChunk(listOf("g"), "ge"))
        // Không khớp gì thì lấy cụm đầu bài chứ không trả rỗng.
        assertEquals("bl", patternForChunk(listOf("bl", "cl"), "xx"))
        assertEquals("", patternForChunk(emptyList(), "sh"))
    }

    /** `word` → chuỗi mảnh của từng ký tự, và chuỗi ký tự hồng, cho dễ đọc khi test đỏ. */
    private fun trace(word: String, chunks: List<String>, patternIndex: Int, pattern: String): Pair<String, String> {
        val letters = clusterLetters(word, BlendSplit(chunks, patternIndex), pattern)
        val chunkTrace = letters.joinToString("") { if (it.chunkIndex == SPACE_CHUNK) "_" else "${it.chunkIndex}" }
        val pinkTrace = letters.filter { it.isPink }.joinToString("") { it.char.toString() }
        return chunkTrace to pinkTrace
    }

    @Test
    fun `xếp ký tự vào mảnh và tô hồng đúng cụm đang dạy`() {
        // Cụm đầu từ.
        assertEquals("00111" to "bl", trace("black", listOf("bl", "ack"), 0, "bl"))
        assertEquals("000111" to "spr", trace("spring", listOf("spr", "ing"), 0, "spr"))
        // Cụm cuối từ.
        assertEquals("0011" to "sh", trace("fish", listOf("fi", "sh"), 1, "sh"))
        assertEquals("0011" to "nd", trace("wind", listOf("wi", "nd"), 1, "nd"))
        // Cụm giữa từ — ba mảnh (chốt câu 3).
        assertEquals("001122" to "th", trace("mother", listOf("mo", "th", "er"), 1, "th"))
        assertEquals("0001122" to "ph", trace("dolphin", listOf("dol", "ph", "in"), 1, "ph"))
        assertEquals("001122" to "ck", trace("rocket", listOf("ro", "ck", "et"), 1, "ck"))
        // Mảnh mang `e` câm: chỉ chữ pattern hồng, `e` xanh.
        assertEquals("0011" to "c", trace("rice", listOf("ri", "ce"), 1, "c"))
        assertEquals("000011" to "g", trace("orange", listOf("oran", "ge"), 1, "g"))
        assertEquals("0011" to "s", trace("rose", listOf("ro", "se"), 1, "s"))
        // `tch` là MỘT mảnh ba chữ.
        assertEquals("00111" to "tch", trace("watch", listOf("wa", "tch"), 1, "tch"))
        // `sc·hool` — sách tô `sc` dù đọc /sk/.
        assertEquals("001111" to "sc", trace("school", listOf("sc", "hool"), 0, "sc"))
    }

    @Test
    fun `từ hai chữ giữ dấu cách làm chỗ trống riêng`() {
        assertEquals("011_22222" to "c", trace("ice cream", listOf("i", "ce", "cream"), 1, "c"))
        assertEquals("0111_22222" to "c", trace("cell phone", listOf("c", "ell", "phone"), 0, "c"))
    }

    @Test
    fun `bảng tách lệch với từ thì bị bắt, không âm thầm hiện sai`() {
        assertTrue(splitMatchesWord("black", BlendSplit(listOf("bl", "ack"), 0)))
        assertTrue(splitMatchesWord("ice cream", BlendSplit(listOf("i", "ce", "cream"), 1)), "dấu cách bỏ khi đối chiếu")
        assertFalse(splitMatchesWord("black", BlendSplit(listOf("bl", "ck"), 0)), "thiếu chữ")
        assertFalse(splitMatchesWord("black", BlendSplit(listOf("bl", "ack"), 2)), "patternIndex trỏ ra ngoài")
        assertFalse(splitMatchesWord("black", BlendSplit(emptyList(), 0)))
    }

    @Test
    fun `thiếu bảng tách thì cả từ là một mảnh, vẫn tô được cụm`() {
        val split = wholeWordSplit("ice cream")
        assertTrue(splitMatchesWord("ice cream", split))
        assertEquals("000_00000" to "c", trace("ice cream", split.chunks, 0, "c"))
    }

    @Test
    fun `cụm không có trong mảnh thì tô cả mảnh chứ không bỏ trống`() {
        assertEquals("0011" to "sh", trace("fish", listOf("fi", "sh"), 1, "zz"))
    }

    // ---- Cấp 5 -------------------------------------------------------------------------
    // Bảng dưới chép tay từ `data/level_5/phonics.csv` (cột `split1..4`, `silent`), dựng
    // theo bản scan sách OPW5. Cấp 5 KHÔNG có bài nào kiểu phép cộng.

    @Test
    fun `cấp 5 - mọi cụm đều là kiểu một âm, không có phép cộng`() {
        val patterns = listOf(
            "ar", "ir", "ur", "er", "or", "ou", "ow", "oi", "oy", "oo", "u",
            "au", "aw", "all", "wa", "oar", "are", "air", "ea", "ear", "eer",
            "a", "e", "i", "o", "kn", "wr", "mb", "rh", "st", "ture", "sure",
            "tion", "sion", "ous", "ful",
        )
        for (pattern in patterns) {
            assertEquals(ClusterKind.Single, clusterKind(pattern, level = 5), "pattern=$pattern")
            assertEquals(emptyList(), equationOperands(pattern, level = 5), "pattern=$pattern")
        }
    }

    @Test
    fun `cùng một cụm vẫn cộng được ở cấp 4 - luật mới không kéo lùi cấp cũ`() {
        // `st` là bài phép cộng của cấp 4 (`s + t = st`) nhưng là chữ CÂM ở cấp 5 (`whistle`).
        assertEquals(ClusterKind.Addition, clusterKind("st", level = 4))
        assertEquals(listOf("s", "t"), equationOperands("st", level = 4))
        // Không truyền cấp thì giữ nguyên hành vi cấp 4 — mọi chỗ gọi cũ không đổi nghĩa.
        assertEquals(ClusterKind.Addition, clusterKind("st"))
        assertEquals(listOf("s", "t"), equationOperands("st"))
    }

    @Test
    fun `cấp 5 - xếp ký tự vào mảnh và tô hồng đúng tổ hợp đang dạy`() {
        // Vần r-controlled: onset · vần · coda.
        assertEquals("011" to "ar", trace("car", listOf("c", "ar"), 1, "ar"))
        assertEquals("0112" to "ar", trace("farm", listOf("f", "ar", "m"), 1, "ar"))
        assertEquals("0011" to "ar", trace("star", listOf("st", "ar"), 1, "ar"))
        // Vần nằm CUỐI từ nhiều âm tiết.
        assertEquals("0000011" to "er", trace("teacher", listOf("teach", "er"), 1, "er"))
        // Nguyên âm âm tiết mở: chữ pattern đứng riêng một mảnh, ở đầu hoặc giữa từ.
        assertEquals("01111" to "a", trace("acorn", listOf("a", "corn"), 0, "a"))
        assertEquals("00001222" to "a", trace("elevator", listOf("elev", "a", "tor"), 1, "a"))
        // Schwa ở cuối từ.
        assertEquals("00001" to "a", trace("panda", listOf("pand", "a"), 1, "a"))
        // Hậu tố — cả mảnh là pattern.
        assertEquals("0001111" to "tion", trace("station", listOf("sta", "tion"), 1, "tion"))
        assertEquals("000111" to "ous", trace("famous", listOf("fam", "ous"), 1, "ous"))
    }

    @Test
    fun `cấp 5 - chữ câm đánh dấu riêng, vẫn thuộc cụm đang dạy`() {
        // `knife`: `k` câm, `n` kêu — cả hai đều là chữ của cụm `kn` nên đều hồng.
        val knife = clusterLetters("knife", BlendSplit(listOf("kn", "ife"), 0), "kn", setOf(0))
        assertEquals("kn", knife.filter { it.isPink }.joinToString("") { it.char.toString() })
        assertEquals("k", knife.filter { it.isSilent }.joinToString("") { it.char.toString() })
        assertTrue(knife[0].isPink, "chữ câm vẫn là chữ của cụm đang dạy")
        // Thẻ dòng 1 vẽ `kn` với ký tự thứ 0 nhạt.
        assertEquals(setOf(0), silentPatternOffsets(knife))

        // `lamb`: `b` câm nằm CUỐI cụm `mb` → ký tự thứ 1 của thẻ.
        val lamb = clusterLetters("lamb", BlendSplit(listOf("la", "mb"), 1), "mb", setOf(3))
        assertEquals("b", lamb.filter { it.isSilent }.joinToString("") { it.char.toString() })
        assertEquals(setOf(1), silentPatternOffsets(lamb))

        // `glove`: bài `ve`, và từ này KHÔNG tách mảnh (user chốt 2026-09-30) — cả từ là
        // một mảnh. Hồng vẫn chỉ hai chữ `ve` chứ không phải cả mảnh: màu lấy theo chỗ
        // pattern NẰM TRONG mảnh. Nếu ai đó đổi `pinkOffsets` thành "cả mảnh pattern"
        // thì `glove` sẽ hồng nguyên từ — test này chặn đúng chỗ đó.
        val glove = clusterLetters("glove", BlendSplit(listOf("glove"), 0), "ve", setOf(4))
        assertEquals("ve", glove.filter { it.isPink }.joinToString("") { it.char.toString() })
        assertEquals("glo", glove.filterNot { it.isPink }.joinToString("") { it.char.toString() })
        assertEquals("e", glove.filter { it.isSilent }.joinToString("") { it.char.toString() })
        assertEquals(setOf(1), silentPatternOffsets(glove))

        // `rhubarb` có hai chữ `b` mà không chữ nào câm — chữ câm là `h` ở vị trí 1.
        // Đây là lý do chỉ số tính theo TỪ chứ không tìm ký tự trong cả từ.
        val rhubarb = clusterLetters("rhubarb", BlendSplit(listOf("rh", "ubarb"), 0), "rh", setOf(1))
        assertEquals("h", rhubarb.filter { it.isSilent }.joinToString("") { it.char.toString() })
        assertEquals(setOf(1), silentPatternOffsets(rhubarb))

        // `whistle`: cụm `st` ở GIỮA từ, `t` câm.
        val whistle = clusterLetters("whistle", BlendSplit(listOf("whi", "st", "le"), 1), "st", setOf(4))
        assertEquals("st", whistle.filter { it.isPink }.joinToString("") { it.char.toString() })
        assertEquals(setOf(1), silentPatternOffsets(whistle))
    }

    @Test
    fun `không khai chữ câm thì không ký tự nào nhạt`() {
        val car = clusterLetters("car", BlendSplit(listOf("c", "ar"), 1), "ar")
        assertTrue(car.none { it.isSilent })
        assertEquals(emptySet(), silentPatternOffsets(car))
    }
}
