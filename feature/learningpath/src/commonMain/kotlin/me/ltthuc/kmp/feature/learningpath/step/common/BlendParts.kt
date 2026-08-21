package me.ltthuc.kmp.feature.learningpath.step.common

/** Vai trò của một mảnh khi đánh vần — quyết định màu chữ và file audio nào được gọi. */
internal enum class BlendPieceKind { Onset, Vowel, Coda, Letter }

/**
 * Một mảnh trong chuỗi đánh vần của từ.
 *
 * [spans] là vị trí ký tự trong CHÍNH từ đó, vì màn hình cấp 3 giữ từ nguyên khối rồi
 * phóng to đúng phần đang đọc chứ không tách thành thẻ rời như cấp 2. Magic-e cho HAI
 * khoảng rời nhau — `tape` đọc /eɪ/ thì phóng to cả `a` lẫn `e` cuối, bỏ qua `p` ở giữa.
 */
internal data class BlendPiece(
    val label: String,
    val spans: List<IntRange>,
    val kind: BlendPieceKind,
)

/**
 * Tách một từ thành chuỗi mảnh để đánh vần ở bước 0 cấp 3.
 *
 * Luật thường: `onset + nguyên âm + phần còn lại`, mỗi mảnh đọc thành MỘT âm.
 *     tape  -> t · a_e · p        (magic-e: nguyên âm gồm `a` và `e` cuối, rời nhau)
 *     game  -> g · a_e · m        (vần bài là "ame" nhưng từ vẫn tách theo magic-e)
 *     rain  -> r · ai · n
 *     bay   -> b · ay             (không có phần đuôi)
 *     skate -> sk · a_e · t       (cụm phụ âm đầu đọc liền khối)
 *     eat   -> ea · t             (không có onset)
 *
 * Tám từ HAI ÂM TIẾT không vào khuôn đó nên tra bảng [TWO_SYLLABLE]: đánh vần từng chữ
 * theo âm chữ cái như cấp 1, riêng vần đọc nguyên khối, và chữ cái đôi chỉ đọc một lần
 * (`happy` = h·a·p·y chứ không phải h·a·p·p·y).
 *
 * Trả rỗng nếu không tìm ra nguyên âm nào — chỗ gọi phải coi đó là lỗi dữ liệu chứ đừng
 * lặng lẽ hiển thị từ trống.
 */
internal fun blendParts(word: String): List<BlendPiece> {
    val w = word.lowercase()
    TWO_SYLLABLE[w]?.let { return it.toPieces(w) }

    val nucleus = findNucleus(w) ?: return emptyList()
    val pieces = mutableListOf<BlendPiece>()

    val onsetEnd = nucleus.spans.first().first
    if (onsetEnd > 0) {
        pieces += BlendPiece(w.substring(0, onsetEnd), listOf(0 until onsetEnd), BlendPieceKind.Onset)
    }
    pieces += nucleus

    // Phần đuôi = mọi thứ sau nguyên âm, TRỪ chữ `e` câm đã tính vào nguyên âm.
    val codaStart = nucleus.spans.first().last + 1
    val codaEnd = if (nucleus.spans.size > 1) nucleus.spans.last().first else w.length
    if (codaStart < codaEnd) {
        pieces += BlendPiece(w.substring(codaStart, codaEnd), listOf(codaStart until codaEnd), BlendPieceKind.Coda)
    }
    return pieces
}

/**
 * Nguyên âm của từ, thử magic-e trước rồi mới tới tổ hợp nguyên âm.
 *
 * Phải thử magic-e TRƯỚC: `time` vừa khớp magic-e (i…e) vừa chứa tổ hợp `ie` nếu quét
 * xuôi, mà `ie` ở đây không phải một tổ hợp — nó là hai nguyên âm bị chữ `m` tách ra.
 */
private fun findNucleus(w: String): BlendPiece? {
    magicE(w)?.let { return it }
    // Tổ hợp dài khớp trước, nếu không thì `ea` nuốt mất `eat` trước khi thử `ee`.
    for (team in VOWEL_TEAMS) {
        val i = w.indexOf(team)
        if (i >= 0) return BlendPiece(team, listOf(i until i + team.length), BlendPieceKind.Vowel)
    }
    // `y` đứng làm nguyên âm (`spy`, `my`) chỉ tính khi nó KHÔNG ở đầu từ — `yak` thì
    // `y` là phụ âm.
    val y = w.lastIndexOf('y')
    if (y > 0) return BlendPiece("y", listOf(y..y), BlendPieceKind.Vowel)
    val v = w.indexOfFirst { it in SHORT_VOWELS }
    if (v >= 0) return BlendPiece(w[v].toString(), listOf(v..v), BlendPieceKind.Vowel)
    return null
}

/**
 * Nguyên âm tách đôi kiểu magic-e: nguyên âm + ÍT NHẤT một phụ âm + `e` cuối từ.
 *
 * Đòi hỏi có phụ âm ở giữa để `bee`/`tie` không bị nhận nhầm — ở hai từ đó `e` cuối
 * không hề câm, nó là một nửa của tổ hợp nguyên âm.
 */
private fun magicE(w: String): BlendPiece? {
    if (w.length < 4 || !w.endsWith('e')) return null
    val last = w.lastIndex
    val vowel = (last - 2 downTo 0).firstOrNull { w[it] in SHORT_VOWELS } ?: return null
    val between = w.substring(vowel + 1, last)
    if (between.isEmpty() || between.any { it in SHORT_VOWELS }) return null
    return BlendPiece("${w[vowel]}_e", listOf(vowel..vowel, last..last), BlendPieceKind.Vowel)
}

/** Dựng mảnh từ bảng chữ đã chốt: mỗi phần tử là một mảnh đọc, khớp lần lượt vào từ. */
private fun List<String>.toPieces(w: String): List<BlendPiece> {
    var cursor = 0
    return map { label ->
        // Chữ cái đôi ("pp", "ll") chỉ đọc một lần nhưng vẫn phải phủ CẢ HAI ký tự,
        // nếu không thì chữ thứ hai không bao giờ được phóng to.
        val doubled = cursor + 1 < w.length && label.length == 1 &&
            w[cursor] == label[0] && w[cursor + 1] == label[0]
        val len = if (doubled) 2 else label.length
        val span = cursor until (cursor + len)
        val atWordStart = cursor == 0
        cursor += len
        // `y` chỉ là nguyên âm khi KHÔNG đứng đầu từ: `yellow` mở đầu bằng phụ âm /j/,
        // còn `candy` kết thúc bằng nguyên âm /i/.
        val kind = when {
            label.any { it in SHORT_VOWELS } -> BlendPieceKind.Vowel
            label == "y" && !atWordStart -> BlendPieceKind.Vowel
            else -> BlendPieceKind.Letter
        }
        BlendPiece(label, listOf(span), kind)
    }
}

private const val SHORT_VOWELS = "aeiou"

/**
 * Tổ hợp nguyên âm của cấp 3, XẾP DÀI TRƯỚC vì phép khớp lấy tổ hợp đầu tiên tìm thấy.
 * `igh` phải đứng trước `ie`, nếu không `light` sẽ không khớp gì cả.
 */
private val VOWEL_TEAMS = listOf(
    "igh", "ai", "ay", "ee", "ea", "ie", "oa", "ow", "ue", "ui", "ew", "oo", "ey",
)

/**
 * Tám từ hai âm tiết, đánh vần từng chữ theo âm chữ cái cấp 1 — chép đúng theo bản chốt
 * của user, KHÔNG suy ra bằng thuật toán.
 *
 * Lý do phải chép tay:
 *   - `money` chữ `o` đọc /ʌ/ chứ không phải /ɒ/ như âm chữ O, nên máy suy sẽ sai âm
 *   - `Tuesday` có TỚI HAI tổ hợp nguyên âm (`ue` giữa từ và `ay` cuối), không lọt khuôn
 *   - chữ cái đôi ở `happy`, `yellow`, `pillow` chỉ đọc một lần
 */
private val TWO_SYLLABLE: Map<String, List<String>> = mapOf(
    "candy" to listOf("c", "a", "n", "d", "y"),
    "happy" to listOf("h", "a", "p", "y"),
    "money" to listOf("m", "o", "n", "ey"),
    "yellow" to listOf("y", "e", "l", "ow"),
    "pillow" to listOf("p", "i", "l", "ow"),
    "elbow" to listOf("e", "l", "b", "ow"),
    "window" to listOf("w", "i", "n", "d", "ow"),
    "tuesday" to listOf("t", "ue", "s", "d", "ay"),
)
