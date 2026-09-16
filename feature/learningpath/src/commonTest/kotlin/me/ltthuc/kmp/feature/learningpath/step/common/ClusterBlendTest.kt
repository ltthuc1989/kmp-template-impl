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
}
