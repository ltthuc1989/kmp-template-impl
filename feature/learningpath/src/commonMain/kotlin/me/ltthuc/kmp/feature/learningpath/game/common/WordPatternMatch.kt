package me.ltthuc.kmp.feature.learningpath.game.common

import me.ltthuc.kmp.feature.learningpath.step.common.BlendPieceKind
import me.ltthuc.kmp.feature.learningpath.step.common.blendParts

/**
 * Từ có chứa vần đang hỏi không — luật cho vòng chơi bằng TỪ của Bubble Pop cấp 3.
 *
 * Hai luật, chọn theo HÌNH DẠNG vần chứ không theo bảng liệt kê:
 *
 *     vần magic-e có phụ âm    ame ake ate ave ime ike ive ine   →  từ kết thúc bằng vần
 *     mọi vần còn lại          a_e ai ay ee y igh ie oa ow oo …  →  vần là nhãn mảnh
 *                                                                  nguyên âm của từ
 *
 * Vì sao phải hai luật: `game` tách ra `g · a_e · m` (xem [blendParts]) — nhãn nguyên âm
 * là `a_e`, KHÔNG phải `ame`. Vần `ame` là vần của BÀI, còn `a_e` là cách viết nguyên âm
 * trong TỪ. Hỏi `ame` mà tra nhãn nguyên âm thì không từ nào khớp.
 *
 * Ngược lại `igh` cũng có phụ âm nhưng KHÔNG dùng luật đuôi được: `light` kết thúc bằng
 * `ght` chứ không phải `igh`. Nó là nhãn nguyên âm thật của từ, nên đi luật dưới.
 *
 * Nhận diện theo hình dạng chứ không liệt kê tay, vì cấp 4-5 còn thêm vần và mỗi lần
 * liệt kê tay là một lần quên. Golden test khoá cả 27 vần cấp 3.
 */
internal fun wordHasPattern(word: String, pattern: String): Boolean {
    val w = word.trim().lowercase()
    val p = pattern.trim().lowercase()
    if (w.isEmpty() || p.isEmpty()) return false
    return if (p.isMagicERime()) {
        w.endsWith(p)
    } else {
        blendParts(w).any { it.kind == BlendPieceKind.Vowel && it.label == p }
    }
}

/**
 * Vần kiểu `ame` `ike`: nguyên âm + MỘT phụ âm + `e` câm.
 *
 * Gạch dưới bị loại trừ tường minh: `a_e` cũng khớp khuôn "nguyên âm + ký tự khác + e"
 * nhưng nó là cách VIẾT nguyên âm, không phải vần đọc lên được — cho nó đi luật đuôi thì
 * `tape` phải kết thúc bằng "a_e", không từ nào khớp và vòng chơi rỗng.
 */
private fun String.isMagicERime(): Boolean =
    '_' !in this && length == 3 && this[0] in VOWELS && this[1] !in VOWELS && this[2] == 'e'

private const val VOWELS = "aeiou"
