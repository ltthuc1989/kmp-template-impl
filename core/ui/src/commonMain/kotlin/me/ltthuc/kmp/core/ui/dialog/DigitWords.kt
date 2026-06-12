package me.ltthuc.kmp.core.ui.dialog

import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.digit_word_0
import me.ltthuc.kmp.core.resource.digit_word_1
import me.ltthuc.kmp.core.resource.digit_word_2
import me.ltthuc.kmp.core.resource.digit_word_3
import me.ltthuc.kmp.core.resource.digit_word_4
import me.ltthuc.kmp.core.resource.digit_word_5
import me.ltthuc.kmp.core.resource.digit_word_6
import me.ltthuc.kmp.core.resource.digit_word_7
import me.ltthuc.kmp.core.resource.digit_word_8
import me.ltthuc.kmp.core.resource.digit_word_9
import org.jetbrains.compose.resources.StringResource

/**
 * Maps a digit 0-9 to its spelled-out, localizable word resource.
 * Compose Resources has no string-array, so 10 keyed strings indexed by digit is the
 * idiomatic, fully-localizable approach.
 */
internal fun digitWordRes(digit: Int): StringResource = when (digit) {
    0 -> Res.string.digit_word_0
    1 -> Res.string.digit_word_1
    2 -> Res.string.digit_word_2
    3 -> Res.string.digit_word_3
    4 -> Res.string.digit_word_4
    5 -> Res.string.digit_word_5
    6 -> Res.string.digit_word_6
    7 -> Res.string.digit_word_7
    8 -> Res.string.digit_word_8
    9 -> Res.string.digit_word_9
    else -> Res.string.digit_word_0
}
