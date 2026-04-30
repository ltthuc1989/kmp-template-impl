@file:Suppress("ClassOrdering")

package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Letter stroke data for tracing guides. Paths authored in normalized 0..100 viewBox per
 * Zaner-Bloser US primary handwriting standard. Rendered via Compose PathParser + scaled
 * at draw time to actual canvas size.
 *
 * Stroke count reflects natural pen lifts as taught in kindergarten. Badge colors rotate
 * red/blue/orange/green so kids can visually follow order 1→2→3→4.
 */
@Immutable
internal data class LetterStroke(
    val svgPath: String,
    val badgeAt: Offset,
    val badgeColor: Color,
)

@Immutable
internal data class LetterGuide(
    val char: Char,
    val uppercase: Boolean,
    val strokes: List<LetterStroke>,
)

internal object LetterPaths {

    private val BADGE_RED = Color(0xFFEF4444)
    private val BADGE_BLUE = Color(0xFF3B82F6)
    private val BADGE_ORANGE = Color(0xFFF97316)
    private val BADGE_GREEN = Color(0xFF10B981)

    private val BADGE_COLORS = listOf(BADGE_RED, BADGE_BLUE, BADGE_ORANGE, BADGE_GREEN)

    private fun stroke(svg: String, badge: Offset, idx: Int) = LetterStroke(
        svgPath = svg,
        badgeAt = badge,
        badgeColor = BADGE_COLORS[idx % BADGE_COLORS.size],
    )

    fun get(char: Char, uppercase: Boolean): LetterGuide {
        val key = char.uppercaseChar()
        return GUIDES[key to uppercase] ?: FALLBACK
    }

    private val FALLBACK = LetterGuide(
        char = '?',
        uppercase = true,
        strokes = listOf(stroke("M 20 50 L 80 50", Offset(50f, 40f), 0)),
    )

    @Suppress("LargeClass", "LongMethod")
    private val GUIDES: Map<Pair<Char, Boolean>, LetterGuide> = buildMap {
        put('A' to true, upperA())
        put('B' to true, upperB())
        put('C' to true, upperC())
        put('D' to true, upperD())
        put('E' to true, upperE())
        put('F' to true, upperF())
        put('G' to true, upperG())
        put('H' to true, upperH())
        put('I' to true, upperI())
        put('J' to true, upperJ())
        put('K' to true, upperK())
        put('L' to true, upperL())
        put('M' to true, upperM())
        put('N' to true, upperN())
        put('O' to true, upperO())
        put('P' to true, upperP())
        put('Q' to true, upperQ())
        put('R' to true, upperR())
        put('S' to true, upperS())
        put('T' to true, upperT())
        put('U' to true, upperU())
        put('V' to true, upperV())
        put('W' to true, upperW())
        put('X' to true, upperX())
        put('Y' to true, upperY())
        put('Z' to true, upperZ())

        put('A' to false, lowerA())
        put('B' to false, lowerB())
        put('C' to false, lowerC())
        put('D' to false, lowerD())
        put('E' to false, lowerE())
        put('F' to false, lowerF())
        put('G' to false, lowerG())
        put('H' to false, lowerH())
        put('I' to false, lowerI())
        put('J' to false, lowerJ())
        put('K' to false, lowerK())
        put('L' to false, lowerL())
        put('M' to false, lowerM())
        put('N' to false, lowerN())
        put('O' to false, lowerO())
        put('P' to false, lowerP())
        put('Q' to false, lowerQ())
        put('R' to false, lowerR())
        put('S' to false, lowerS())
        put('T' to false, lowerT())
        put('U' to false, lowerU())
        put('V' to false, lowerV())
        put('W' to false, lowerW())
        put('X' to false, lowerX())
        put('Y' to false, lowerY())
        put('Z' to false, lowerZ())
    }

    // ---------------- Uppercase (cap height 10..90) ----------------

    private fun upperA() = LetterGuide(
        'A',
        true,
        listOf(
            stroke("M 50 10 L 20 90", Offset(33f, 45f), 0),
            stroke("M 50 10 L 80 90", Offset(67f, 45f), 1),
            stroke("M 32 62 L 68 62", Offset(50f, 62f), 2),
        ),
    )

    private fun upperB() = LetterGuide(
        'B',
        true,
        listOf(
            stroke("M 28 10 L 28 90", Offset(20f, 50f), 0),
            stroke("M 28 10 C 70 10 70 50 28 50", Offset(55f, 22f), 1),
            stroke("M 28 50 C 78 50 78 90 28 90", Offset(60f, 62f), 2),
        ),
    )

    private fun upperC() = LetterGuide(
        'C',
        true,
        listOf(
            stroke("M 78 22 C 55 5 22 20 22 50 C 22 80 55 95 78 78", Offset(50f, 15f), 0),
        ),
    )

    private fun upperD() = LetterGuide(
        'D',
        true,
        listOf(
            stroke("M 28 10 L 28 90", Offset(20f, 50f), 0),
            stroke("M 28 10 C 80 10 80 90 28 90", Offset(65f, 20f), 1),
        ),
    )

    private fun upperE() = LetterGuide(
        'E',
        true,
        listOf(
            stroke("M 28 10 L 28 90", Offset(20f, 50f), 0),
            stroke("M 28 10 L 78 10", Offset(50f, 4f), 1),
            stroke("M 28 50 L 68 50", Offset(48f, 44f), 2),
            stroke("M 28 90 L 78 90", Offset(50f, 96f), 3),
        ),
    )

    private fun upperF() = LetterGuide(
        'F',
        true,
        listOf(
            stroke("M 28 10 L 28 90", Offset(20f, 50f), 0),
            stroke("M 28 10 L 78 10", Offset(50f, 4f), 1),
            stroke("M 28 50 L 68 50", Offset(48f, 44f), 2),
        ),
    )

    private fun upperG() = LetterGuide(
        'G',
        true,
        listOf(
            stroke("M 78 22 C 55 5 22 20 22 50 C 22 80 55 95 78 78", Offset(50f, 15f), 0),
            stroke("M 78 60 L 78 80", Offset(85f, 62f), 1),
            stroke("M 55 60 L 78 60", Offset(67f, 54f), 2),
        ),
    )

    private fun upperH() = LetterGuide(
        'H',
        true,
        listOf(
            stroke("M 25 10 L 25 90", Offset(17f, 50f), 0),
            stroke("M 75 10 L 75 90", Offset(83f, 50f), 1),
            stroke("M 25 50 L 75 50", Offset(50f, 44f), 2),
        ),
    )

    private fun upperI() = LetterGuide(
        'I',
        true,
        listOf(
            stroke("M 30 10 L 70 10", Offset(50f, 4f), 0),
            stroke("M 50 10 L 50 90", Offset(42f, 50f), 1),
            stroke("M 30 90 L 70 90", Offset(50f, 96f), 2),
        ),
    )

    private fun upperJ() = LetterGuide(
        'J',
        true,
        listOf(
            stroke("M 35 10 L 80 10", Offset(60f, 4f), 0),
            stroke("M 62 10 L 62 75 C 62 92 35 92 25 78", Offset(70f, 45f), 1),
        ),
    )

    private fun upperK() = LetterGuide(
        'K',
        true,
        listOf(
            stroke("M 25 10 L 25 90", Offset(17f, 50f), 0),
            stroke("M 75 10 L 25 55", Offset(55f, 25f), 1),
            stroke("M 40 45 L 78 90", Offset(60f, 62f), 2),
        ),
    )

    private fun upperL() = LetterGuide(
        'L',
        true,
        listOf(
            stroke("M 30 10 L 30 90", Offset(22f, 50f), 0),
            stroke("M 30 90 L 78 90", Offset(55f, 96f), 1),
        ),
    )

    private fun upperM() = LetterGuide(
        'M',
        true,
        listOf(
            stroke("M 20 10 L 20 90", Offset(12f, 50f), 0),
            stroke("M 20 10 L 50 60", Offset(30f, 32f), 1),
            stroke("M 80 10 L 50 60", Offset(70f, 32f), 2),
            stroke("M 80 10 L 80 90", Offset(88f, 50f), 3),
        ),
    )

    private fun upperN() = LetterGuide(
        'N',
        true,
        listOf(
            stroke("M 22 10 L 22 90", Offset(14f, 50f), 0),
            stroke("M 22 10 L 78 90", Offset(50f, 52f), 1),
            stroke("M 78 10 L 78 90", Offset(86f, 50f), 2),
        ),
    )

    private fun upperO() = LetterGuide(
        'O',
        true,
        listOf(
            stroke("M 50 10 C 20 10 20 90 50 90 C 80 90 80 10 50 10", Offset(30f, 22f), 0),
        ),
    )

    private fun upperP() = LetterGuide(
        'P',
        true,
        listOf(
            stroke("M 25 10 L 25 90", Offset(17f, 50f), 0),
            stroke("M 25 10 C 72 10 72 52 25 52", Offset(58f, 20f), 1),
        ),
    )

    private fun upperQ() = LetterGuide(
        'Q',
        true,
        listOf(
            stroke("M 50 10 C 20 10 20 90 50 90 C 80 90 80 10 50 10", Offset(30f, 22f), 0),
            stroke("M 60 70 L 88 95", Offset(78f, 80f), 1),
        ),
    )

    private fun upperR() = LetterGuide(
        'R',
        true,
        listOf(
            stroke("M 25 10 L 25 90", Offset(17f, 50f), 0),
            stroke("M 25 10 C 72 10 72 52 25 52", Offset(58f, 20f), 1),
            stroke("M 40 52 L 78 90", Offset(60f, 65f), 2),
        ),
    )

    private fun upperS() = LetterGuide(
        'S',
        true,
        listOf(
            stroke(
                "M 78 22 C 55 5 22 12 22 32 C 22 52 78 48 78 68 C 78 88 45 95 25 78",
                Offset(60f, 15f),
                0,
            ),
        ),
    )

    private fun upperT() = LetterGuide(
        'T',
        true,
        listOf(
            stroke("M 15 10 L 85 10", Offset(50f, 4f), 0),
            stroke("M 50 10 L 50 90", Offset(42f, 50f), 1),
        ),
    )

    private fun upperU() = LetterGuide(
        'U',
        true,
        listOf(
            stroke("M 22 10 L 22 70 C 22 92 78 92 78 70 L 78 10", Offset(14f, 40f), 0),
        ),
    )

    private fun upperV() = LetterGuide(
        'V',
        true,
        listOf(
            stroke("M 18 10 L 50 90", Offset(30f, 40f), 0),
            stroke("M 50 90 L 82 10", Offset(70f, 40f), 1),
        ),
    )

    private fun upperW() = LetterGuide(
        'W',
        true,
        listOf(
            stroke("M 12 10 L 28 90", Offset(18f, 40f), 0),
            stroke("M 28 90 L 50 30", Offset(38f, 55f), 1),
            stroke("M 50 30 L 72 90", Offset(62f, 55f), 2),
            stroke("M 72 90 L 88 10", Offset(82f, 40f), 3),
        ),
    )

    private fun upperX() = LetterGuide(
        'X',
        true,
        listOf(
            stroke("M 20 10 L 80 90", Offset(35f, 35f), 0),
            stroke("M 80 10 L 20 90", Offset(65f, 35f), 1),
        ),
    )

    private fun upperY() = LetterGuide(
        'Y',
        true,
        listOf(
            stroke("M 20 10 L 50 50", Offset(30f, 25f), 0),
            stroke("M 80 10 L 50 50", Offset(70f, 25f), 1),
            stroke("M 50 50 L 50 90", Offset(42f, 70f), 2),
        ),
    )

    private fun upperZ() = LetterGuide(
        'Z',
        true,
        listOf(
            stroke("M 20 10 L 78 10", Offset(50f, 4f), 0),
            stroke("M 78 10 L 22 90", Offset(50f, 52f), 1),
            stroke("M 22 90 L 80 90", Offset(50f, 96f), 2),
        ),
    )

    // ---------------- Lowercase (x-height band 42..88, ascender 10, descender 100) ----------------

    private fun lowerA() = LetterGuide(
        'a',
        false,
        listOf(
            stroke(
                "M 68 55 C 55 42 30 45 28 62 C 26 82 55 92 68 78",
                Offset(42f, 48f),
                0,
            ),
            stroke("M 68 50 L 68 88", Offset(76f, 68f), 1),
        ),
    )

    private fun lowerB() = LetterGuide(
        'b',
        false,
        listOf(
            stroke("M 28 10 L 28 88", Offset(20f, 50f), 0),
            stroke(
                "M 28 58 C 45 45 75 50 75 70 C 75 92 42 92 28 82",
                Offset(52f, 50f),
                1,
            ),
        ),
    )

    private fun lowerC() = LetterGuide(
        'c',
        false,
        listOf(
            stroke(
                "M 72 55 C 55 42 28 48 28 65 C 28 82 55 92 72 78",
                Offset(50f, 46f),
                0,
            ),
        ),
    )

    private fun lowerD() = LetterGuide(
        'd',
        false,
        listOf(
            stroke(
                "M 70 55 C 55 42 28 48 28 65 C 28 82 55 92 70 78",
                Offset(62f, 58f),
                0,
            ),
            stroke("M 70 10 L 70 88", Offset(78f, 20f), 1),
        ),
    )

    private fun lowerE() = LetterGuide(
        'e',
        false,
        listOf(
            stroke(
                "M 28 64 L 68 64 C 76 62 76 42 55 42 C 30 42 22 62 28 78 C 32 92 58 92 72 78",
                Offset(48f, 62f),
                0,
            ),
        ),
    )

    private fun lowerF() = LetterGuide(
        'f',
        false,
        listOf(
            stroke("M 72 22 C 58 10 42 18 42 32 L 42 88", Offset(55f, 15f), 0),
            stroke("M 25 48 L 60 48", Offset(42f, 42f), 1),
        ),
    )

    private fun lowerG() = LetterGuide(
        'g',
        false,
        listOf(
            stroke(
                "M 68 55 C 55 42 30 48 30 65 C 30 82 55 92 68 78",
                Offset(48f, 48f),
                0,
            ),
            stroke("M 68 50 L 68 85 C 68 100 42 102 28 90", Offset(75f, 75f), 1),
        ),
    )

    private fun lowerH() = LetterGuide(
        'h',
        false,
        listOf(
            stroke("M 28 10 L 28 88", Offset(20f, 50f), 0),
            stroke("M 28 58 C 40 46 72 46 72 66 L 72 88", Offset(52f, 52f), 1),
        ),
    )

    private fun lowerI() = LetterGuide(
        'i',
        false,
        listOf(
            stroke("M 50 48 L 50 88", Offset(42f, 68f), 0),
            stroke("M 50 30 L 50 32", Offset(58f, 31f), 1),
        ),
    )

    private fun lowerJ() = LetterGuide(
        'j',
        false,
        listOf(
            stroke("M 55 48 L 55 85 C 55 100 35 102 22 90", Offset(65f, 70f), 0),
            stroke("M 55 30 L 55 32", Offset(63f, 31f), 1),
        ),
    )

    private fun lowerK() = LetterGuide(
        'k',
        false,
        listOf(
            stroke("M 28 10 L 28 88", Offset(20f, 50f), 0),
            stroke("M 68 52 L 28 72", Offset(52f, 58f), 1),
            stroke("M 40 64 L 72 88", Offset(58f, 75f), 2),
        ),
    )

    private fun lowerL() = LetterGuide(
        'l',
        false,
        listOf(
            stroke("M 50 10 L 50 88", Offset(42f, 50f), 0),
        ),
    )

    private fun lowerM() = LetterGuide(
        'm',
        false,
        listOf(
            stroke("M 18 48 L 18 88", Offset(10f, 68f), 0),
            stroke("M 18 58 C 25 46 48 46 48 62 L 48 88", Offset(30f, 52f), 1),
            stroke("M 48 58 C 55 46 82 46 82 62 L 82 88", Offset(62f, 52f), 2),
        ),
    )

    private fun lowerN() = LetterGuide(
        'n',
        false,
        listOf(
            stroke("M 28 48 L 28 88", Offset(20f, 68f), 0),
            stroke("M 28 58 C 40 46 72 46 72 62 L 72 88", Offset(52f, 52f), 1),
        ),
    )

    private fun lowerO() = LetterGuide(
        'o',
        false,
        listOf(
            stroke("M 50 45 C 28 45 28 88 50 88 C 72 88 72 45 50 45", Offset(32f, 52f), 0),
        ),
    )

    private fun lowerP() = LetterGuide(
        'p',
        false,
        listOf(
            stroke("M 28 48 L 28 100", Offset(20f, 78f), 0),
            stroke(
                "M 28 58 C 42 45 75 48 75 68 C 75 88 42 92 28 78",
                Offset(52f, 50f),
                1,
            ),
        ),
    )

    private fun lowerQ() = LetterGuide(
        'q',
        false,
        listOf(
            stroke(
                "M 72 55 C 58 42 28 48 28 65 C 28 82 58 92 72 78",
                Offset(48f, 48f),
                0,
            ),
            stroke("M 72 48 L 72 100", Offset(80f, 80f), 1),
        ),
    )

    private fun lowerR() = LetterGuide(
        'r',
        false,
        listOf(
            stroke("M 28 48 L 28 88", Offset(20f, 68f), 0),
            stroke("M 28 60 C 40 48 60 48 68 58", Offset(48f, 48f), 1),
        ),
    )

    private fun lowerS() = LetterGuide(
        's',
        false,
        listOf(
            stroke(
                "M 68 55 C 55 42 32 45 32 55 C 32 68 68 68 68 78 C 68 90 40 92 28 80",
                Offset(52f, 48f),
                0,
            ),
        ),
    )

    private fun lowerT() = LetterGuide(
        't',
        false,
        listOf(
            stroke("M 42 25 L 42 80 C 42 88 55 90 68 82", Offset(50f, 40f), 0),
            stroke("M 25 48 L 58 48", Offset(42f, 42f), 1),
        ),
    )

    private fun lowerU() = LetterGuide(
        'u',
        false,
        listOf(
            stroke("M 28 48 L 28 75 C 28 92 55 92 60 78", Offset(20f, 68f), 0),
            stroke("M 60 48 L 60 88", Offset(68f, 68f), 1),
        ),
    )

    private fun lowerV() = LetterGuide(
        'v',
        false,
        listOf(
            stroke("M 25 48 L 50 88", Offset(35f, 65f), 0),
            stroke("M 50 88 L 75 48", Offset(65f, 65f), 1),
        ),
    )

    private fun lowerW() = LetterGuide(
        'w',
        false,
        listOf(
            stroke("M 15 48 L 30 88", Offset(20f, 68f), 0),
            stroke("M 30 88 L 45 55", Offset(38f, 72f), 1),
            stroke("M 45 55 L 60 88", Offset(52f, 72f), 2),
            stroke("M 60 88 L 75 48", Offset(70f, 68f), 3),
        ),
    )

    private fun lowerX() = LetterGuide(
        'x',
        false,
        listOf(
            stroke("M 28 48 L 72 88", Offset(40f, 58f), 0),
            stroke("M 72 48 L 28 88", Offset(60f, 58f), 1),
        ),
    )

    private fun lowerY() = LetterGuide(
        'y',
        false,
        listOf(
            stroke("M 28 48 L 50 75", Offset(35f, 58f), 0),
            stroke("M 72 48 L 38 100", Offset(60f, 70f), 1),
        ),
    )

    private fun lowerZ() = LetterGuide(
        'z',
        false,
        listOf(
            stroke("M 28 48 L 72 48", Offset(50f, 42f), 0),
            stroke("M 72 48 L 28 88", Offset(50f, 68f), 1),
            stroke("M 28 88 L 72 88", Offset(50f, 94f), 2),
        ),
    )
}
