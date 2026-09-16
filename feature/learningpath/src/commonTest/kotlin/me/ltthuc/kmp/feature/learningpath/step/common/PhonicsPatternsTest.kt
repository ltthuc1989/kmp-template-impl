package me.ltthuc.kmp.feature.learningpath.step.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden test cho [parsePatterns] — khoá bất biến mà mọi tên file audio dựa vào.
 *
 * Bảng dưới đây chép tay từ `opw_audio_project/data/level_3/phonics.csv` và phải
 * khớp từng dòng với `displayLetter` trong `curriculum.json`. KHÔNG sinh bảng từ
 * chính parser — làm vậy thì test chỉ lặp lại bug của parser.
 *
 * Bản song sinh phía Python: `opw_audio_project/tests/test_parse_patterns.py`.
 * Sửa một bên thì chạy cả hai.
 */
class PhonicsPatternsTest {

    @Test
    fun `cấp 3 - mã letter tách đúng thành pattern`() {
        val golden = listOf(
            "A_E" to listOf("a_e"),
            "A_E-AME-AKE" to listOf("ame", "ake"),
            "A_E-ATE-AVE" to listOf("ate", "ave"),
            "I_E" to listOf("i_e"),
            "I_E-IME-IKE" to listOf("ime", "ike"),
            "I_E-IVE-INE" to listOf("ive", "ine"),
            "O_E" to listOf("o_e"),
            "U_E-1" to listOf("u_e"),
            "U_E-2" to listOf("u_e"),
            "AI" to listOf("ai"),
            "AY" to listOf("ay"),
            "AI-AY" to listOf("ai", "ay"),
            "EE" to listOf("ee"),
            "EA" to listOf("ea"),
            "Y-EY" to listOf("y", "ey"),
            "IGH" to listOf("igh"),
            "IE" to listOf("ie"),
            "Y" to listOf("y"),
            "OA" to listOf("oa"),
            "OW" to listOf("ow"),
            "OA-OW" to listOf("oa", "ow"),
            "UE" to listOf("ue"),
            "UI-EW" to listOf("ui", "ew"),
            "OO" to listOf("oo"),
        )
        assertEquals(24, golden.size, "Cấp 3 có đúng 24 lesson")
        for ((letter, want) in golden) {
            assertEquals(want, parsePatterns(letter, 3), "letter=$letter")
        }
    }

    @Test
    fun `cấp 4 - mã letter tách đúng thành pattern`() {
        // Chép tay từ `data/level_4/phonics.csv` sau khi đổi mã 2026-09-07
        // (TH-voiced→TH-1, TH-unvoiced→TH-2, SOFT-C→C, SOFT-G→G, VOICED-S→S).
        val golden = listOf(
            "BL-CL" to listOf("bl", "cl"),
            "BR-CR" to listOf("br", "cr"),
            "FL-GL" to listOf("fl", "gl"),
            "FR-GR" to listOf("fr", "gr"),
            "PL-SL" to listOf("pl", "sl"),
            "DR-TR" to listOf("dr", "tr"),
            "SM-SN" to listOf("sm", "sn"),
            "SP-SW" to listOf("sp", "sw"),
            "ST" to listOf("st"),
            "SH" to listOf("sh"),
            "CH-TCH" to listOf("ch", "tch"),
            "PH-WH" to listOf("ph", "wh"),
            "TH-1" to listOf("th"),
            "TH-2" to listOf("th"),
            "CK-QU" to listOf("ck", "qu"),
            "NG-NK" to listOf("ng", "nk"),
            "ND-NT" to listOf("nd", "nt"),
            "LT-MP" to listOf("lt", "mp"),
            "SK-SC" to listOf("sk", "sc"),
            "SPR-STR" to listOf("spr", "str"),
            "SPL-SQU" to listOf("spl", "squ"),
            "C" to listOf("c"),
            "G" to listOf("g"),
            "S" to listOf("s"),
        )
        assertEquals(24, golden.size, "Cấp 4 có đúng 24 lesson")
        for ((letter, want) in golden) {
            assertEquals(want, parsePatterns(letter, 4), "letter=$letter")
        }
    }

    @Test
    fun `mã cũ có từ mô tả không còn được chấp nhận - phải đổi dữ liệu chứ không vá parser`() {
        // Bảo vệ quyết định 2026-09-07: dữ liệu L4 đã đổi sang mã sạch. Nếu ai đưa lại mã
        // kiểu "TH-voiced" thì test này đỏ để nhắc, thay vì parser âm thầm đẻ pattern "voiced".
        assertEquals(listOf("th", "voiced"), parsePatterns("TH-voiced", 4))
    }

    @Test
    fun `cấp 2 giữ nguyên hành vi cũ - lesson nguyên âm đơn trả rỗng`() {
        assertEquals(emptyList(), parsePatterns("SHORT-A", 2))
        assertEquals(listOf("am"), parsePatterns("SHORT-A-AM", 2))
        assertEquals(listOf("ad", "ag"), parsePatterns("SHORT-A-AD-AG", 2))
        assertEquals(listOf("ib", "id"), parsePatterns("SHORT-I-IB-ID", 2))
    }

    @Test
    fun `cấp 1 - chữ cái đơn không sinh vần nào`() {
        assertEquals(emptyList(), parsePatterns("A", 1))
        assertEquals(emptyList(), parsePatterns("Z", 1))
    }

    @Test
    fun `mã hỏng hoặc thừa ký tự không làm vỡ parser`() {
        assertEquals(emptyList(), parsePatterns("", 3))
        assertEquals(emptyList(), parsePatterns("---", 3))
        assertEquals(listOf("ai", "ay"), parsePatterns("  AI-AY  ", 3))
        assertEquals(listOf("u_e"), parsePatterns("U_E-1-2", 3), "nhiều đoạn số vẫn bỏ hết")
    }
}
