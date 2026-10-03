package me.ltthuc.kmp.feature.learningpath.step.common

import me.ltthuc.kmp.core.model.BlendSplit

/**
 * Hai kiểu panel "Listen and learn" của cấp 4 — chia theo NGỮ ÂM chứ không theo unit.
 *
 * [Addition] phép cộng: hai/ba âm rời ghép lại thành cụm (`b + l = bl`, `s + p + r = spr`).
 * [Single]   một chữ ghép = MỘT âm, không cộng được (`s` + `h` ≠ /ʃ/) nên dòng 1 chỉ hiện
 *            đúng cụm đó.
 *
 * Sách xếp 10 bài vào [Single]: sh · ch tch · ph wh · th ×2 · ck qu · ng nk · c · g · s.
 * `ck qu ng nk` cùng unit với các bài phép cộng nhưng vẫn là một âm (/k/ /kw/ /ŋ/ /ŋk/),
 * còn `nd nt lt mp` là hai âm rời nên vẫn cộng được dù nằm cuối từ (chốt câu 1, 2026-09-07).
 */
internal enum class ClusterKind { Addition, Single }

/**
 * Cụm nào đọc thành MỘT âm. Bảng chép tay theo sách, không suy bằng thuật toán: `sc`
 * (hai âm) và `ck` (một âm) nhìn giống nhau về mặt chuỗi ký tự, chỉ ngữ âm mới phân biệt được.
 *
 * Phía audio phải dùng đúng bảng này khi dựng nhịp dòng 1 (P2) — lệch nhau là màn hình
 * hiện `s + h = sh` trong lúc tiếng chỉ đọc "sh", không có lỗi nào nổ ra.
 */
private val SINGLE_SOUND_PATTERNS = setOf(
    "sh", "ch", "tch", "ph", "wh", "th", "ck", "qu", "ng", "nk", "c", "g", "s",
)

/**
 * Cấp đầu tiên mà MỌI bài đều là [ClusterKind.Single].
 *
 * Cấp 5 dạy tổ hợp chữ cái (`ar`, `ou`, `tion`, `kn`), không cụm nào cộng được từ âm của
 * từng chữ: `a` + `r` không ra /ɑr/, `k` + `n` không ra /n/ (k còn câm). Sách OPW5 cũng
 * không in dấu `+` ở bài nào — panel "Listen and learn" của nó luôn là một thẻ pattern.
 * Nên gác theo CẤP thay vì nhét 40 cụm mới vào [SINGLE_SOUND_PATTERNS]: bảng đó nói "cụm
 * này đọc thành một âm", còn đây là một luật của cả cấp.
 */
internal const val ALL_SINGLE_SOUND_LEVEL = 5

/** Cấp mặc định khi chỗ gọi không biết cấp — cấp 4, nơi hai kiểu panel cùng tồn tại. */
private const val DEFAULT_CLUSTER_LEVEL = 4

internal fun clusterKind(pattern: String, level: Int = DEFAULT_CLUSTER_LEVEL): ClusterKind = when {
    level >= ALL_SINGLE_SOUND_LEVEL -> ClusterKind.Single
    pattern.trim().lowercase() in SINGLE_SOUND_PATTERNS -> ClusterKind.Single
    else -> ClusterKind.Addition
}

/**
 * Các toán hạng của dòng 1: `bl` → `b` + `l`, `spr` → `s` + `p` + `r`, `squ` → `s` + `qu`.
 *
 * `qu` giữ nguyên khối vì `q` một mình không có âm trong tiếng Anh — sách cũng in `s + qu = squ`.
 * Bài kiểu [ClusterKind.Single] trả rỗng: dòng 1 của nó chỉ có một thẻ, không có phép cộng.
 * Từ cấp [ALL_SINGLE_SOUND_LEVEL] trở lên luôn rỗng.
 */
internal fun equationOperands(pattern: String, level: Int = DEFAULT_CLUSTER_LEVEL): List<String> {
    val p = pattern.trim().lowercase()
    if (p.isEmpty() || clusterKind(p, level) == ClusterKind.Single) return emptyList()
    if (p.length > QU.length && p.endsWith(QU)) {
        return p.dropLast(QU.length).map { it.toString() } + QU
    }
    return p.map { it.toString() }
}

/**
 * Bài dạy hai cụm thì mỗi từ theo một cụm — chọn bằng MẢNH PATTERN của bảng tách, không
 * bằng `endsWith` như cấp 3.
 *
 * Cấp 4 có cụm ở đầu, giữa và cuối từ ngay trong một bài (`shell` đầu, `fish` cuối), nên
 * đoán theo vị trí là sai một nửa số từ — đúng lỗi #2 của bản soát 2026-08-31. Mảnh pattern
 * đã do người duyệt chốt trong CSV nên nó là chỗ tra duy nhất đáng tin.
 *
 * Thử cụm DÀI trước: `watch` có mảnh `tch`, mà `tch` cũng chứa `ch`.
 */
internal fun patternForChunk(patterns: List<String>, chunk: String): String {
    val c = chunk.lowercase()
    return patterns.sortedByDescending { it.length }.firstOrNull { c.contains(it.lowercase()) }
        ?: patterns.firstOrNull().orEmpty()
}

/**
 * Bảng tách có khớp với từ không: ghép mọi mảnh lại (bỏ dấu cách) phải ra đúng từ, và
 * `patternIndex` phải trỏ vào một mảnh có thật.
 *
 * Dữ liệu lệch thì màn hình rơi về [wholeWordSplit] chứ không hiện chữ sai vị trí — nhưng
 * chỗ gọi PHẢI log, vì đây là lỗi dữ liệu im lặng: bé vẫn thấy một màn chạy được, chỉ là
 * không còn dạy đúng cụm nào cả.
 */
internal fun splitMatchesWord(word: String, split: BlendSplit): Boolean =
    split.patternIndex in split.chunks.indices &&
        split.chunks.joinToString("").lowercase() == word.lowercase().filterNot { it == ' ' }

/** Đường lui khi thiếu bảng tách: cả từ là một mảnh, vẫn đọc được, chỉ không tách. */
internal fun wholeWordSplit(word: String): BlendSplit =
    BlendSplit(chunks = listOf(word.filterNot { it == ' ' }), patternIndex = 0)

/** Chỉ số mảnh của dấu cách — dấu cách không thuộc mảnh nào, chỉ giữ chỗ trên hàng chữ. */
internal const val SPACE_CHUNK = -1

/**
 * Một ký tự trên hàng chữ: thuộc mảnh nào (để hiện/ẩn theo nhịp đọc) và có phải chữ của
 * cụm đang dạy không (để tô hồng).
 */
internal data class ClusterLetter(
    val char: Char,
    val chunkIndex: Int,
    val isPink: Boolean,
    /**
     * Chữ CÂM — vẽ hồng nhạt (cấp 5 unit 7: `k` của `knife`, `b` của `lamb`, `e` của `glove`).
     *
     * Luôn là chữ của cụm đang dạy nên đi kèm [isPink]; nhạt để bé thấy "chữ này không kêu".
     */
    val isSilent: Boolean = false,
)

/**
 * Xếp từng ký tự của từ vào mảnh của nó, và đánh dấu chữ nào tô hồng.
 *
 * Hồng = đúng những ký tự của [pattern] NẰM TRONG mảnh pattern, không phải cả mảnh: `rice`
 * tách `ri`·`ce` với pattern `c`, mảnh mang `e` câm để đọc thành một tiếng nhưng chỉ `c`
 * được tô (chốt câu 3). Cũng không phải "mọi chỗ xuất hiện trong từ": `church` có hai `ch`
 * mà bài chỉ dạy cái nằm trong mảnh đã chốt.
 *
 * Dấu cách giữ một chỗ hẹp ([SPACE_CHUNK]) để `ice cream` không dính thành `icecream`.
 */
internal fun clusterLetters(
    word: String,
    split: BlendSplit,
    pattern: String,
    silentIndices: Set<Int> = emptySet(),
): List<ClusterLetter> {
    val pinkOffsets = pinkOffsets(split.patternChunk, pattern)
    var chunk = 0
    var used = 0
    return word.mapIndexed { index, ch ->
        if (ch == ' ') return@mapIndexed ClusterLetter(ch, SPACE_CHUNK, isPink = false)
        // Nhảy mảnh khi mảnh hiện tại đã đủ ký tự. Dừng ở mảnh cuối để ký tự thừa (dữ liệu
        // lệch mà lọt qua [splitMatchesWord]) vẫn có chỗ đứng thay vì văng chỉ số.
        while (chunk < split.chunks.lastIndex && used >= split.chunks[chunk].length) {
            chunk++
            used = 0
        }
        val pink = chunk == split.patternIndex && used in pinkOffsets
        used++
        ClusterLetter(ch, chunk, pink, isSilent = index in silentIndices)
    }
}

/**
 * Chỉ số (trong chuỗi [pattern]) của những chữ câm, để thẻ dòng 1 vẽ `k n` với `k` nhạt.
 *
 * Suy từ chính [letters] chứ không tính lại từ chỉ số trong từ: những ký tự tô hồng CHÍNH LÀ
 * các chữ của pattern, đúng thứ tự — nên vị trí thứ i trong nhóm hồng là ký tự thứ i của thẻ.
 * Tính lại bằng `pattern.indexOf(ch)` thì sai ở cụm có chữ lặp.
 */
internal fun silentPatternOffsets(letters: List<ClusterLetter>): Set<Int> =
    letters.filter { it.isPink }
        .withIndex()
        .filter { it.value.isSilent }
        .map { it.index }
        .toSet()

/**
 * Vị trí (trong MẢNH) của những ký tự thuộc cụm đang dạy.
 *
 * Không tìm thấy cụm trong mảnh là dữ liệu đã lệch — tô cả mảnh còn hơn không tô gì, vì
 * một từ không có chữ hồng nào thì bé không biết bài đang dạy cái gì.
 */
private fun pinkOffsets(chunk: String, pattern: String): Set<Int> {
    val p = pattern.trim().lowercase()
    val at = if (p.isEmpty()) -1 else chunk.lowercase().indexOf(p)
    if (at < 0) return chunk.indices.toSet()
    return (at until at + p.length).toSet()
}

private const val QU = "qu"
