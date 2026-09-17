package me.ltthuc.kmp.feature.learningpath.game.common

import me.ltthuc.kmp.core.model.PhonicsLesson

/**
 * Những vần ÔM TRỌN unit trong [patterns]: mọi từ của unit đều khớp.
 *
 * `a_e` của unit 1 và `i_e` của unit 2 cấp 3 là vần BAO — `tape` `game` `cake` … đều tách ra
 * nguyên âm `a_e`. Vòng chơi theo vần đó không có "từ của riêng nó", nên user chốt bỏ khỏi
 * Bubble Pop (2026-08-31) và khỏi Fill Letter (2026-09-17). Hai game gọi chung hàm này để
 * không lệch luật.
 *
 * Nhận diện bằng dữ liệu chứ không liệt kê `a_e`/`i_e` bằng tay: cấp sau còn vần bao khác, và
 * mỗi lần liệt kê tay là một lần quên. Unit 3 không có vần bao vì `o_e` chỉ khớp 4/12 từ và
 * `u_e` khớp 8/12.
 *
 * Unit không có từ nào trả rỗng — "mọi từ đều khớp" trên danh sách rỗng là đúng về logic
 * nhưng sẽ loại sạch mọi vần.
 */
internal fun List<PhonicsLesson>.umbrellaPatterns(patterns: Collection<String>): Set<String> {
    val allWords = flatMap { it.words }.map { it.word }
    if (allWords.isEmpty()) return emptySet()
    return patterns.filterTo(mutableSetOf()) { p -> allWords.all { wordHasPattern(it, p) } }
}
