package me.ltthuc.kmp.feature.learningpath.game.filletter

import me.ltthuc.kmp.core.model.BlendSplit
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.maskWord
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Golden test cho Fill Letter kiểu khuyết vần (chốt 2026-09-17).
 *
 * Mã `letter`, `displayLetter`, từ và bảng tách chép tay từ `curriculum.json` — KHÔNG sinh từ
 * chính hàm đang test. Soát đủ mọi từ của 5 cấp thì chạy `scripts/build_fill_chunk_preview.py`.
 *
 * Sai luật này không nổ lỗi nào: bé thấy ô trống sai chỗ (`_ox` hỏi chữ f trong bài chữ X — lỗi
 * của bản cũ), hoặc thẻ đáp án lộ ra vì dài hơn ba thẻ còn lại.
 */
class FillChunkTest {

    private val l4 = listOf(
        labels("L4U4_sh" to "SH", "L4U4_ch_tch" to "CH-TCH", "L4U4_ph_wh" to "PH-WH"),
        labels("L4U5_th_voiced" to "TH-1", "L4U5_th_unvoiced" to "TH-2", "L4U5_ck_qu" to "CK-QU"),
        labels("L4U6_ng_nk" to "NG-NK", "L4U6_nd_nt" to "ND-NT", "L4U6_lt_mp" to "LT-MP"),
        labels("L4U7_sk_sc" to "SK-SC", "L4U7_spr_str" to "SPR-STR", "L4U7_spl_squ" to "SPL-SQU"),
        labels("L4U8_soft_c" to "C", "L4U8_soft_g" to "G", "L4U8_voiced_s" to "S"),
    )

    // ------------------------------------------------------------------ phần khuyết

    @Test
    fun level1BlanksTheLessonLetterWhereItStands() {
        // Bản cũ luôn che chữ đầu: `fox` thành `_ox`, đáp án `f` — không phải chữ X đang học.
        assertChunk(lesson("L1U8_X", "X", "Xx", word("fox")), "fox", "x", listOf(2..2), ChunkKind.Letter)
        assertChunk(lesson("L1U1_A", "A", "Aa", word("apple")), "apple", "a", listOf(0..0), ChunkKind.Letter)
    }

    @Test
    fun level2BlanksVowelOrFinalRime() {
        assertChunk(lesson("L2U1_a", "SHORT-A", "a", word("cat")), "cat", "a", listOf(1..1), ChunkKind.Vowel)
        assertChunk(lesson("L2U1_am", "SHORT-A-AM", "am", word("ram")), "ram", "am", listOf(1..2), ChunkKind.Rime)
        assertChunk(
            lesson("L2U2_ad_ag", "SHORT-A-AD-AG", "ad ag", word("bag")),
            "bag",
            "ag",
            listOf(1..2),
            ChunkKind.Rime,
        )
    }

    @Test
    fun level3BlanksVowelTeamRimeOrSplitMagicE() {
        assertChunk(
            lesson("L3U1_ame_ake", "A_E-AME-AKE", "ame ake", word("game")),
            "game",
            "ame",
            listOf(1..3),
            ChunkKind.Rime,
        )
        // Magic-e tách: HAI ô, thẻ `o_e` điền cả hai.
        assertChunk(lesson("L3U3_o_e", "O_E", "o_e", word("home")), "home", "o_e", listOf(1..1, 3..3), ChunkKind.SplitVowel)
        assertChunk(lesson("L3U4_ai", "AI", "ai", word("rain")), "rain", "ai", listOf(1..2), ChunkKind.Vowel)
        assertChunk(lesson("L3U5_y_ey", "Y-EY", "y ey", word("candy")), "candy", "y", listOf(4..4), ChunkKind.Vowel)
        assertChunk(lesson("L3U5_y_ey", "Y-EY", "y ey", word("money")), "money", "ey", listOf(3..4), ChunkKind.Vowel)
        assertChunk(lesson("L3U6_igh", "IGH", "igh", word("light")), "light", "igh", listOf(1..3), ChunkKind.Vowel)
    }

    @Test
    fun level3WordsCarryingOnlyTheUmbrellaRimeAreSkippedOnPurpose() {
        val tape = lesson("L3U1_a_e", "A_E", "a_e", word("tape"))
        assertEquals(ChunkLookup.Umbrella, fillChunkFor(tape, tape.words.single(), umbrella = setOf("a_e")))
        // Unit không có vần bao thì chính từ đó vẫn chơi được.
        assertIs<ChunkLookup.Found>(fillChunkFor(tape, tape.words.single(), umbrella = emptySet()))
    }

    @Test
    fun level4BlanksThePatternLettersOfTheSplit() {
        assertChunk(
            lesson("L4U5_th_voiced", "TH-1", "th", word("father", "fa", "th", "er", patternIndex = 1)),
            "father",
            "th",
            listOf(2..3),
            ChunkKind.Cluster,
        )
        // Mảnh `tch` cũng chứa `ch` — phải chọn vần dài.
        assertChunk(
            lesson("L4U4_ch_tch", "CH-TCH", "ch tch", word("watch", "wa", "tch", patternIndex = 1)),
            "watch",
            "tch",
            listOf(2..4),
            ChunkKind.Cluster,
        )
        // Mảnh mang `e` câm nhưng chỉ `c` bị che.
        assertChunk(
            lesson("L4U8_soft_c", "C", "c", word("rice", "ri", "ce", patternIndex = 1)),
            "rice",
            "c",
            listOf(2..2),
            ChunkKind.Cluster,
        )
        // Split bỏ dấu cách: vị trí phải quy về từ gốc có dấu cách.
        assertChunk(
            lesson("L4U8_soft_c", "C", "c", word("ice cream", "i", "ce", "cream", patternIndex = 1)),
            "ice cream",
            "c",
            listOf(1..1),
            ChunkKind.Cluster,
        )
        assertChunk(
            lesson("L4U4_ph_wh", "PH-WH", "ph wh", word("dolphin", "dol", "ph", "in", patternIndex = 1)),
            "dolphin",
            "ph",
            listOf(3..4),
            ChunkKind.Cluster,
        )
    }

    @Test
    fun level4PlusWithoutUsableSplitIsBrokenNotGuessed() {
        val noSplit = lesson("L4U5_th_voiced", "TH-1", "th", word("father"))
        assertIs<ChunkLookup.Broken>(fillChunkFor(noSplit, noSplit.words.single()))

        val wrongSplit = lesson("L4U5_th_voiced", "TH-1", "th", word("father", "fa", "th", "or", patternIndex = 1))
        assertIs<ChunkLookup.Broken>(fillChunkFor(wrongSplit, wrongSplit.words.single()))

        // `pencil` có cả e lẫn i; dò chuỗi sẽ che e, trong khi âm ơ nằm ở i. Không có split thì không đoán.
        val pencil = lesson("L5U6_schwa_eiou", "SCHWA-EIOU", "e i o u", word("pencil"))
        assertIs<ChunkLookup.Broken>(fillChunkFor(pencil, pencil.words.single()))
    }

    // ------------------------------------------------------------------ thẻ của lesson

    @Test
    fun lessonLabelsFollowTheLevelCode() {
        assertEquals(
            listOf(ChunkLabel("a", ChunkKind.Letter), ChunkLabel("a", ChunkKind.Vowel)),
            lesson("L1U1_A", "A", "Aa").fillLabels(),
        )
        assertEquals(listOf(ChunkLabel("b", ChunkKind.Letter)), lesson("L1U1_B", "B", "Bb").fillLabels())
        assertEquals(listOf(ChunkLabel("a", ChunkKind.Vowel)), lesson("L2U1_a", "SHORT-A", "a").fillLabels())
        assertEquals(
            listOf(ChunkLabel("ad", ChunkKind.Rime), ChunkLabel("ag", ChunkKind.Rime)),
            lesson("L2U2_ad_ag", "SHORT-A-AD-AG", "ad ag").fillLabels(),
        )
        assertEquals(
            listOf(ChunkLabel("ame", ChunkKind.Rime), ChunkLabel("ake", ChunkKind.Rime)),
            lesson("L3U1_ame_ake", "A_E-AME-AKE", "ame ake").fillLabels(),
        )
        assertEquals(listOf(ChunkLabel("o_e", ChunkKind.SplitVowel)), lesson("L3U3_o_e", "O_E", "o_e").fillLabels())
        assertEquals(listOf(ChunkLabel("th", ChunkKind.Cluster)), lesson("L4U5_th_voiced", "TH-1", "th").fillLabels())
        // Cấp 5: mã `letter` mang nhãn mô tả — phải đọc displayLetter, không ra "schwa".
        assertEquals(
            listOf("e", "i", "o", "u"),
            lesson("L5U6_schwa_eiou", "SCHWA-EIOU", "e i o u").fillLabels().map { it.label },
        )
    }

    @Test
    fun lengthGroupIgnoresTheMagicEJoiner() {
        assertFalse(isLongLabel("th"))
        assertFalse(isLongLabel("a_e"))
        assertTrue(isLongLabel("tch"))
        assertTrue(isLongLabel("tion"))
    }

    // ------------------------------------------------------------------ kho nhiễu

    @Test
    fun unitShortOfChoicesBorrowsFromThePreviousUnit() {
        // Ví dụ user đưa: father → th; unit có th · ck · qu, mượn thêm từ L4U4. `th` hai bài là một thẻ.
        val tiers = distractorTiers(chunk("th", ChunkKind.Cluster), unitIndex = 1, unitLabels = l4)
        assertEquals(listOf("ck", "qu"), tiers[0])
        // `tch` là thẻ dài — không được đứng cạnh ba thẻ hai chữ.
        assertEquals(listOf("sh", "ch", "ph", "wh"), tiers[1])
    }

    @Test
    fun choicesShareTheAnswersLengthGroup() {
        // `c` của L4U8 mượn thẻ NGẮN của L4U7 (sk sc), không phải spr / str.
        val soft = distractorTiers(chunk("c", ChunkKind.Cluster), unitIndex = 4, unitLabels = l4)
        assertEquals(listOf("g", "s"), soft[0])
        assertEquals(listOf("sk", "sc"), soft[1])

        // `tch` không có thẻ dài nào đã học → lấy thẻ dài của unit sau.
        val tch = distractorTiers(chunk("tch", ChunkKind.Cluster), unitIndex = 0, unitLabels = l4)
        assertEquals(listOf("spr", "str", "spl", "squ"), tch.first())
        assertTrue(tch.flatten().all { isLongLabel(it) })
    }

    @Test
    fun answerWithoutSameKindPeersFallsBackToAnyKindOfTheSameLength() {
        val l3 = listOf(
            labels("L3U2_i_e" to "I_E", "L3U2_ime_ike" to "I_E-IME-IKE", "L3U2_ive_ine" to "I_E-IVE-INE"),
            labels("L3U5_ee" to "EE", "L3U5_ea" to "EA", "L3U5_y_ey" to "Y-EY"),
            labels("L3U6_igh" to "IGH", "L3U6_ie" to "IE", "L3U6_y" to "Y"),
        )
        // `igh` là nguyên âm 3 chữ duy nhất → vần magic-e 3 chữ của L3U2, KHÔNG phải ie / y.
        val igh = distractorTiers(chunk("igh", ChunkKind.Vowel), unitIndex = 2, unitLabels = l3)
        assertEquals(listOf(listOf("ime", "ike", "ive", "ine")), igh)

        // `ie`: y của unit, rồi ee ea ey của L3U5 — `y` hai unit là một thẻ.
        val ie = distractorTiers(chunk("ie", ChunkKind.Vowel), unitIndex = 2, unitLabels = l3)
        assertEquals(listOf("y"), ie[0])
        assertEquals(listOf("ee", "ea", "ey"), ie[1])
    }

    @Test
    fun splitMagicENeverMixesWithContiguousChunks() {
        val l3 = listOf(
            labels("L3U1_a_e" to "A_E", "L3U1_ame_ake" to "A_E-AME-AKE"),
            labels("L3U2_i_e" to "I_E", "L3U2_ime_ike" to "I_E-IME-IKE"),
            labels("L3U3_o_e" to "O_E", "L3U3_u_e_1" to "U_E-1", "L3U3_u_e_2" to "U_E-2"),
        )
        val tiers = distractorTiers(chunk("o_e", ChunkKind.SplitVowel), unitIndex = 2, unitLabels = l3)
        assertEquals(listOf(listOf("u_e"), listOf("i_e"), listOf("a_e")), tiers)
    }

    @Test
    fun level2VowelBorrowsLevel1VowelLetters() {
        val units = listOf(
            labels("L1U3_G" to "G", "L1U3_H" to "H", "L1U3_I" to "I"),
            labels("L1U5_M" to "M", "L1U5_N" to "N", "L1U5_O" to "O"),
            labels("L1U7_S" to "S", "L1U7_T" to "T", "L1U7_U" to "U", "L1U7_V" to "V"),
            labels("L1U8_W" to "W", "L1U8_X" to "X", "L1U8_Y" to "Y", "L1U8_Z" to "Z"),
            labels("L2U1_a" to "SHORT-A", "L2U1_am" to "SHORT-A-AM", "L2U1_an" to "SHORT-A-AN"),
            labels("L2U2_ad_ag" to "SHORT-A-AD-AG", "L2U2_ap" to "SHORT-A-AP", "L2U2_at" to "SHORT-A-AT"),
        )
        val vowel = distractorTiers(chunk("a", ChunkKind.Vowel), unitIndex = 4, unitLabels = units)
        assertEquals(listOf(listOf("u"), listOf("o"), listOf("i")), vowel.take(3))

        // Vần am / an: không unit trước nào có vần → lấy unit sau (chốt D5).
        val rime = distractorTiers(chunk("am", ChunkKind.Rime), unitIndex = 4, unitLabels = units)
        assertEquals(listOf("an"), rime[0])
        assertEquals(listOf("ad", "ag", "ap", "at"), rime[1])
    }

    @Test
    fun pickTakesUpperTiersFirst() {
        val tiers = listOf(listOf("ck", "qu"), listOf("sh", "ch", "ph", "wh"))
        repeat(20) { seed ->
            val picked = pickDistractors(tiers, count = 3, random = Random(seed))
            assertEquals(3, picked.size)
            assertTrue(picked.containsAll(listOf("ck", "qu")))
            assertEquals(3, picked.toSet().size)
        }
        assertEquals(listOf("x"), pickDistractors(listOf(listOf("x")), count = 3, random = Random(0)))
    }

    @Test
    fun maskShowsOneBlankPerSpanWhateverItsLength() {
        assertEquals("fa_er", maskWord("father", listOf(2..3)))
        assertEquals("wa_", maskWord("watch", listOf(2..4)))
        assertEquals("h_m_", maskWord("home", listOf(1..1, 3..3)))
        assertEquals("i_e cream", maskWord("ice cream", listOf(1..1)))
    }

    // ------------------------------------------------------------------ dựng dữ liệu

    private fun assertChunk(
        lesson: PhonicsLesson,
        text: String,
        label: String,
        spans: List<IntRange>,
        kind: ChunkKind,
    ) {
        val lookup = fillChunkFor(lesson, lesson.words.first { it.word == text })
        assertIs<ChunkLookup.Found>(lookup, "$text: $lookup")
        assertEquals(FillChunk(label, spans, kind), lookup.chunk, text)
    }

    private fun chunk(label: String, kind: ChunkKind) = FillChunk(label, listOf(0..0), kind)

    /**
     * Thẻ của một unit từ các cặp (id lesson, mã letter). displayLetter lấy đuôi id ("L2U1_a" → "a")
     * — chỉ bài nguyên âm đơn cấp 2 đọc tới nó, các bài còn lại đi theo mã letter.
     */
    private fun labels(vararg lessons: Pair<String, String>): List<ChunkLabel> =
        lessons.flatMap { (id, code) -> lesson(id, code, id.substringAfter('_')).fillLabels() }

    private fun word(text: String, vararg chunks: String, patternIndex: Int = 0) = LessonWord(
        word = text,
        displays = emptyList(),
        blendSplit = if (chunks.isEmpty()) null else BlendSplit(chunks.toList(), patternIndex),
    )

    private fun lesson(id: String, letter: String, displayLetter: String, vararg words: LessonWord) = PhonicsLesson(
        id = id,
        unitId = id.substringBefore('_'),
        letter = letter,
        displayLetter = displayLetter,
        soundSpelling = "",
        sentence = "",
        stretchedWord = "",
        orderIndex = 0,
        words = words.toList(),
        chantTexts = emptyList(),
        chantOrder = emptyList(),
    )
}
