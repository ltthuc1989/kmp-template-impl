package me.ltthuc.kmp.feature.learningpath.game.filletter

import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.feature.learningpath.game.common.isMagicERime
import me.ltthuc.kmp.feature.learningpath.game.common.wordHasPattern
import me.ltthuc.kmp.feature.learningpath.step.common.BlendPieceKind
import me.ltthuc.kmp.feature.learningpath.step.common.blendParts
import me.ltthuc.kmp.feature.learningpath.step.common.lessonPatterns
import me.ltthuc.kmp.feature.learningpath.step.common.level
import me.ltthuc.kmp.feature.learningpath.step.common.splitMatchesWord
import kotlin.random.Random

/*
 * Luật của Fill Letter kiểu khuyết VẦN (chốt 2026-09-17). Trang soát từng từ của cả 5 cấp:
 * `scripts/build_fill_chunk_preview.py` — script port tay đúng luật này, sửa bên nào thì sửa
 * cả bên kia.
 *
 *   1. Đáp án = phần bài dạy nằm trong từ, ở đúng chỗ nó đứng       [fillChunkFor]
 *   2. Nhiễu cùng LOẠI và cùng NHÓM ĐỘ DÀI với đáp án               [distractorTiers]
 *   3. Unit thiếu thì lùi unit trước, hết đường lùi mới sang unit sau
 *   4. Vẫn thiếu thì bỏ ràng buộc loại, giữ nhóm độ dài
 */

/**
 * Loại của một thẻ vần — nhiễu chỉ lấy cùng loại với đáp án.
 *
 * Không có luật này thì `c_t` nhận thẻ `am` (camt) và `r_n` nhận thẻ `ime`. [SplitVowel] tách
 * riêng vì nó là HÌNH DẠNG khác chứ không chỉ là âm khác: `h_m_` có hai ô, thẻ `ai` không nhét
 * vừa.
 */
internal enum class ChunkKind { Letter, Vowel, Rime, SplitVowel, Cluster, Combo }

/** Một thẻ vần mà một lesson đem vào kho nhiễu. */
internal data class ChunkLabel(val label: String, val kind: ChunkKind) {
    val isLong: Boolean get() = isLongLabel(label)
}

/**
 * Phần khuyết của một từ: [label] là chữ trên thẻ đáp án (`th`, `o_e`), [spans] là các khoảng
 * ký tự bị che trong từ gốc — HAI khoảng khi là magic-e tách (`home` che `o` và `e`).
 */
internal data class FillChunk(
    val label: String,
    val spans: List<IntRange>,
    val kind: ChunkKind,
) {
    val isLong: Boolean get() = isLongLabel(label)
}

internal sealed interface ChunkLookup {
    data class Found(val chunk: FillChunk) : ChunkLookup

    /** Cố ý không chơi: từ chỉ mang vần bao `a_e`/`i_e` (chốt D3). Không phải lỗi. */
    data object Umbrella : ChunkLookup

    /**
     * Dữ liệu không khớp luật. Chỗ gọi PHẢI log — bỏ từ mà im lặng thì bé chỉ thấy ít vòng
     * hơn, không ai biết vì sao.
     */
    data class Broken(val reason: String) : ChunkLookup
}

/**
 * Nhóm độ dài: 1–2 chữ là ngắn, 3–4 chữ là dài. 4 thẻ của một vòng phải cùng nhóm (chốt
 * 2026-09-17) — `wa_` với tch · sh · ch · ph thì `tch` lộ ngay vì là thẻ dài duy nhất.
 *
 * Gạch nối của magic-e không tính: `a_e` là hai chữ, cùng nhóm với mọi magic-e tách khác.
 */
internal fun isLongLabel(label: String): Boolean = label.count { it != '_' } >= LONG_LABEL_MIN_LETTERS

/**
 * Các thẻ lesson này đem vào kho nhiễu.
 *
 * Cấp 1: chữ nguyên âm `a e i o u` góp THÊM một thẻ loại [ChunkKind.Vowel] — chữ A cấp 1 dạy
 * đúng âm ngắn "ahh" của cấp 2, nên với bé đó là nguyên âm đã học. Nhờ vậy `c_t` của L2U1 có
 * nhiễu u · o · i thay vì phải lấy của unit chưa học.
 */
internal fun PhonicsLesson.fillLabels(): List<ChunkLabel> {
    val lv = level() ?: 1
    return when {
        lv <= 1 -> {
            val l = letter.trim().lowercase()
            when {
                l.isEmpty() -> emptyList()
                l in VOWELS -> listOf(ChunkLabel(l, ChunkKind.Letter), ChunkLabel(l, ChunkKind.Vowel))
                else -> listOf(ChunkLabel(l, ChunkKind.Letter))
            }
        }
        lv == 2 -> lessonPatterns().ifEmpty { displayTokens() }.map { ChunkLabel(it, chunkKind(it, lv)) }
        lv <= LAST_CODE_PATTERN_LEVEL -> lessonPatterns().map { ChunkLabel(it, chunkKind(it, lv)) }
        else -> displayTokens().map { ChunkLabel(it, chunkKind(it, lv)) }
    }
}

/**
 * Tìm phần khuyết của [word] theo luật của cấp:
 *
 *     L1   chữ cái của bài, lần xuất hiện đầu        fox   → fo_    x
 *     L2   nguyên âm (bài không dạy vần) hoặc vần cuối  cat → c_t  a · ram → r_  am
 *     L3   vần magic-e ở cuối · nguyên âm của blendParts   game → g_ ame · rain → r_n ai
 *          magic-e tách thành HAI ô                     home  → h_m_   o_e
 *     L4+  chữ của pattern nằm trong mảnh pattern của bảng tách split
 *                                                        father → fa_er th · rice → ri_e c
 *
 * Cấp 4+ BẮT BUỘC có split: pattern đứng đầu, giữa hay cuối từ tuỳ bài, và `pencil` (L5) có cả
 * `e` lẫn `i` mà chỉ `i` là âm ơ — dò chuỗi là khuyết sai âm. Thiếu split → [ChunkLookup.Broken].
 *
 * [umbrella] là các vần bao của unit (xem `umbrellaPatterns`); từ chỉ mang vần bao trả
 * [ChunkLookup.Umbrella].
 */
internal fun fillChunkFor(
    lesson: PhonicsLesson,
    word: LessonWord,
    umbrella: Set<String> = emptySet(),
): ChunkLookup {
    val lv = lesson.level() ?: 1
    val lower = word.word.lowercase()
    return when {
        lv <= 1 -> letterChunk(lesson.letter.trim().lowercase(), lower)
        lv == 2 -> level2Chunk(lesson, lower)
        lv == 3 -> level3Chunk(lesson, lower, umbrella)
        else -> splitChunk(lesson, word, lv)
    }
}

/**
 * Kho nhiễu cho [answer], chia TẦNG theo thứ tự lấy: hết tầng trên mới xuống tầng dưới.
 *
 * Trong mỗi lượt quét, tầng đi theo unit: unit của đáp án → lùi dần từng unit trước (qua được
 * level trước) → tiến dần từng unit sau (chưa học — chỉ L1U1 và vần am/an của L2U1 phải tới đây).
 *
 *     lượt 1   cùng loại + cùng nhóm độ dài
 *     lượt 2   mọi loại liền khối + cùng nhóm độ dài — cho đáp án không có bạn cùng loại, như
 *              `igh` (nguyên âm 3 chữ duy nhất): lấy ime · ike · ine của L3U2
 *
 * Magic-e tách không vào lượt 2 theo cả hai chiều: thẻ liền khối không nhét vừa hai ô, và ngược
 * lại.
 *
 * Mặt chữ trùng chỉ giữ lần đầu (`th` hữu thanh và vô thanh là một thẻ), và không bao giờ trùng
 * đáp án. [unitLabels] là thẻ của từng unit theo thứ tự học; [unitIndex] ngoài khoảng thì trả rỗng.
 */
internal fun distractorTiers(
    answer: FillChunk,
    unitIndex: Int,
    unitLabels: List<List<ChunkLabel>>,
): List<List<String>> {
    if (unitIndex !in unitLabels.indices) return emptyList()
    val order = listOf(unitIndex) + (unitIndex - 1 downTo 0) + (unitIndex + 1 until unitLabels.size)
    val seen = mutableSetOf(answer.label)
    val answerSplit = answer.kind == ChunkKind.SplitVowel

    fun sweep(accept: (ChunkLabel) -> Boolean): List<List<String>> = order.mapNotNull { i ->
        unitLabels[i]
            .filter { it.isLong == answer.isLong && accept(it) }
            .map { it.label }
            .filter { seen.add(it) }
            .takeIf { it.isNotEmpty() }
    }

    val sameKind = sweep { it.kind == answer.kind }
    val anyKind = if (answerSplit) emptyList() else sweep { it.kind != ChunkKind.SplitVowel }
    return sameKind + anyKind
}

/** Bốc [count] thẻ: hết tầng trên mới sang tầng dưới, trong một tầng thì bốc ngẫu nhiên. */
internal fun pickDistractors(tiers: List<List<String>>, count: Int, random: Random): List<String> {
    val picked = mutableListOf<String>()
    for (tier in tiers) {
        if (picked.size >= count) break
        picked += tier.shuffled(random).take(count - picked.size)
    }
    return picked
}

private fun letterChunk(label: String, lower: String): ChunkLookup {
    val at = if (label.isEmpty()) -1 else lower.indexOf(label)
    if (at < 0) return ChunkLookup.Broken("không thấy chữ '$label' trong '$lower'")
    return ChunkLookup.Found(FillChunk(label, listOf(at until at + label.length), ChunkKind.Letter))
}

private fun level2Chunk(lesson: PhonicsLesson, lower: String): ChunkLookup {
    val patterns = lesson.lessonPatterns()
    if (patterns.isEmpty()) {
        // Bài nguyên âm đơn ("SHORT-A"): không dạy vần, khuyết chính nguyên âm.
        val vowel = lesson.displayLetter.trim().lowercase()
        val found = letterChunk(vowel, lower)
        return if (found is ChunkLookup.Found) {
            ChunkLookup.Found(found.chunk.copy(kind = ChunkKind.Vowel))
        } else {
            found
        }
    }
    val rime = patterns.filter { lower.endsWith(it) }.maxByOrNull { it.length }
        ?: return ChunkLookup.Broken("'$lower' không kết thúc bằng ${patterns.joinToString("/")}")
    val start = lower.length - rime.length
    return ChunkLookup.Found(FillChunk(rime, listOf(start until lower.length), ChunkKind.Rime))
}

private fun level3Chunk(lesson: PhonicsLesson, lower: String, umbrella: Set<String>): ChunkLookup {
    val patterns = lesson.lessonPatterns()
    val playable = patterns.filterNot { it in umbrella }
    if (patterns.isNotEmpty() && playable.isEmpty()) return ChunkLookup.Umbrella
    // Vần dài thử trước, cùng lý do với bảng tổ hợp nguyên âm của blendParts.
    for (pattern in playable.sortedByDescending { it.length }) {
        if (!wordHasPattern(lower, pattern)) continue
        val kind = chunkKind(pattern, level = 3)
        if (pattern.isMagicERime()) {
            val start = lower.length - pattern.length
            return ChunkLookup.Found(FillChunk(pattern, listOf(start until lower.length), kind))
        }
        val piece = blendParts(lower).firstOrNull { it.kind == BlendPieceKind.Vowel && it.label == pattern }
            ?: continue
        return ChunkLookup.Found(FillChunk(pattern, piece.spans, kind))
    }
    return ChunkLookup.Broken("'$lower' không khớp vần nào trong ${playable.joinToString("/")}")
}

private fun splitChunk(lesson: PhonicsLesson, word: LessonWord, lv: Int): ChunkLookup {
    val text = word.word
    val split = word.blendSplit ?: return ChunkLookup.Broken("'$text' thiếu bảng tách split")
    if (!splitMatchesWord(text, split)) {
        return ChunkLookup.Broken("split ${split.chunks} ghép lại không ra '$text'")
    }
    val patterns = if (lv <= LAST_CODE_PATTERN_LEVEL) lesson.lessonPatterns() else lesson.displayTokens()
    val chunk = split.patternChunk.lowercase()
    // Dài trước: mảnh `tch` của `watch` cũng chứa `ch`.
    val pattern = patterns.sortedByDescending { it.length }.firstOrNull { chunk.contains(it) }
        ?: return ChunkLookup.Broken("mảnh '$chunk' của '$text' không chứa ${patterns.joinToString("/")}")
    // Vị trí trong chuỗi BỎ dấu cách → vị trí trong từ gốc, vì `ice cream` tách không có dấu cách.
    val letterAt = text.indices.filter { text[it] != ' ' }
    val from = split.chunks.take(split.patternIndex).sumOf { it.length } + chunk.indexOf(pattern)
    val start = letterAt.getOrNull(from)
    val end = letterAt.getOrNull(from + pattern.length - 1)
    if (start == null || end == null) return ChunkLookup.Broken("mảnh pattern của '$text' nằm ngoài từ")
    return ChunkLookup.Found(FillChunk(pattern, listOf(start..end), chunkKind(pattern, lv)))
}

private fun chunkKind(label: String, level: Int): ChunkKind = when {
    level <= 1 -> ChunkKind.Letter
    level == 2 -> if (label.length == 1) ChunkKind.Vowel else ChunkKind.Rime
    level == 3 -> when {
        '_' in label -> ChunkKind.SplitVowel
        label.isMagicERime() -> ChunkKind.Rime
        else -> ChunkKind.Vowel
    }
    level <= LAST_CODE_PATTERN_LEVEL -> ChunkKind.Cluster
    else -> ChunkKind.Combo
}

/**
 * Vần đọc từ `displayLetter` ("e i o u" → e, i, o, u). Cấp 5 phải đi đường này: mã `letter`
 * của nó ("A-OPEN", "SCHWA-EIOU") mang nhãn mô tả, `lessonPatterns` đọc ra rác "open", "schwa".
 */
private fun PhonicsLesson.displayTokens(): List<String> =
    displayLetter.trim().lowercase().split(' ').filter { it.isNotEmpty() }

private const val VOWELS = "aeiou"
private const val LONG_LABEL_MIN_LETTERS = 3

/** Cấp cuối cùng mà `lessonPatterns` đọc đúng mã `letter`. */
private const val LAST_CODE_PATTERN_LEVEL = 4
