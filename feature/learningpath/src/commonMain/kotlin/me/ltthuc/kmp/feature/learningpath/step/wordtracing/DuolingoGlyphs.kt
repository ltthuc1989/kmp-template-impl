package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuide
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterStroke

/**
 * Lowercase letter glyphs for Level-2+ word tracing, authored ENTIRELY from the Duolingo ABC
 * tracing references — SEPARATE from Level 1's Zaner-Bloser [me.ltthuc.kmp.feature.learningpath
 * .step.tracing.LetterPaths]. Level 1 keeps LetterPaths; this table is never mixed with it.
 *
 * Authoring rules (must all hold):
 * - NO retrace / NO self-overlap: a stroke never doubles back over its own line. Letters split into
 *   multiple OPEN strokes connecting at shared endpoints; stems drawn top → down. Bowl+stem letters
 *   (a d g q) = a "c"-bowl (open on the right) + a SEPARATE right stem.
 * - Stroke EDGES sit ON the guide lines — the letter must not cross a line. The stroke is thick
 *   (~11 viewBox units, half ≈ 5.5), so CENTERLINES are inset ~5.5 from each guide: x-height
 *   centerlines run ~48..82 (edges touch midline 42 / baseline 88); ascender-top centerline ~15
 *   (edge at topline 10); descender-bottom centerline ~94 (edge at descender line 100). Only the
 *   descenders g j p q y go below the baseline.
 *
 * Coordinate space: normalized 0..100 viewBox. Guides: topline 10, midline 42, baseline 88,
 * descender 100.
 */
internal object DuolingoGlyphs {

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

    private val FALLBACK = LetterGuide(
        char = '?',
        uppercase = false,
        strokes = listOf(stroke("M 26 50 L 74 50", Offset(40f, 44f), 0)),
    )

    /** Lowercase Duolingo glyph for [char]; falls back to a dash for unknown characters. */
    fun get(char: Char): LetterGuide = GLYPHS[char.lowercaseChar()] ?: FALLBACK

    @Suppress("LargeClass", "LongMethod")
    private val GLYPHS: Map<Char, LetterGuide> = buildMap {
        put('a', a())
        put('b', b())
        put('c', c())
        put('d', d())
        put('e', e())
        put('f', f())
        put('g', g())
        put('h', h())
        put('i', i())
        put('j', j())
        put('k', k())
        put('l', l())
        put('m', m())
        put('n', n())
        put('o', o())
        put('p', p())
        put('q', q())
        put('r', r())
        put('s', s())
        put('t', t())
        put('u', u())
        put('v', v())
        put('w', w())
        put('x', x())
        put('y', y())
        put('z', z())
    }

    // 2 strokes: (1) "c"-bowl open on the right, (2) right stem top → baseline.
    private fun a() = LetterGuide(
        'a',
        false,
        listOf(
            stroke(
                "M 67 48 C 55 40 40 40 33 50 C 26 60 27 74 37 80 C 48 84 60 81 67 74",
                Offset(67f, 52f),
                0,
            ),
            stroke("M 67 52 L 67 82", Offset(67f, 53f), 1),
        ),
    )

    // 2 strokes: (1) ascender stem top → baseline, (2) bowl drawn top → down (CW), ends at stem foot.
    private fun b() = LetterGuide(
        'b',
        false,
        listOf(
            stroke("M 30 15 L 30 82", Offset(30f, 17f), 0),
            stroke("M 30 45 C 50 40 70 45 70 63 C 70 82 50 86 30 83", Offset(32f, 46f), 1),
        ),
    )

    // 1 stroke: open C, start top-right, counter-clockwise.
    private fun c() = LetterGuide(
        'c',
        false,
        listOf(
            stroke(
                "M 71 50 C 58 40 37 41 32 57 C 27 71 34 81 47 82 C 58 83 66 80 71 75",
                Offset(71f, 54f),
                0,
            ),
        ),
    )

    // 2 strokes: (1) "c"-bowl open on the right, (2) full ascender stem top → baseline.
    private fun d() = LetterGuide(
        'd',
        false,
        listOf(
            stroke(
                "M 70 48 C 58 40 42 40 35 50 C 27 60 29 74 39 80 C 50 84 62 81 70 74",
                Offset(70f, 52f),
                0,
            ),
            stroke("M 70 15 L 70 82", Offset(70f, 17f), 1),
        ),
    )

    // 1 stroke: mid-left start, horizontal bar, loop CCW, open at bottom-right.
    private fun e() = LetterGuide(
        'e',
        false,
        listOf(
            stroke(
                "M 33 65 L 69 63 C 74 48 64 41 52 41 C 34 41 28 65 34 78 C 40 86 61 86 70 77",
                Offset(34f, 64f),
                0,
            ),
        ),
    )

    // 2 strokes: (1) top hook + stem to baseline, (2) crossbar at midline.
    private fun f() = LetterGuide(
        'f',
        false,
        listOf(
            stroke("M 68 28 C 57 17 43 21 43 33 L 43 82", Offset(63f, 20f), 0),
            stroke("M 28 42 L 62 42", Offset(31f, 37f), 1),
        ),
    )

    // 2 strokes: (1) "c"-bowl, (2) right stem from top down through the baseline into a left tail.
    private fun g() = LetterGuide(
        'g',
        false,
        listOf(
            stroke(
                "M 67 48 C 55 40 40 40 33 50 C 26 60 27 74 37 80 C 48 84 60 81 67 74",
                Offset(67f, 52f),
                0,
            ),
            stroke("M 67 52 L 67 92 C 67 106 45 108 33 98", Offset(67f, 53f), 1),
        ),
    )

    // 2 strokes: (1) stem top → baseline, (2) arch from the upper stem over to the right, down.
    private fun h() = LetterGuide(
        'h',
        false,
        listOf(
            stroke("M 30 15 L 30 82", Offset(30f, 17f), 0),
            stroke("M 30 54 C 34 40 66 40 69 57 L 69 82", Offset(32f, 54f), 1),
        ),
    )

    // 2 strokes: (1) x-height stem, (2) dot.
    private fun i() = LetterGuide(
        'i',
        false,
        listOf(
            stroke("M 50 44 L 50 82", Offset(50f, 46f), 0),
            stroke("M 50 33 L 50 35", Offset(58f, 34f), 1),
        ),
    )

    // 2 strokes: (1) stem into a left-curving descender, (2) dot.
    private fun j() = LetterGuide(
        'j',
        false,
        listOf(
            stroke("M 55 48 L 55 90 C 55 102 39 104 28 96", Offset(55f, 50f), 0),
            stroke("M 55 33 L 55 35", Offset(63f, 34f), 1),
        ),
    )

    // 2 strokes: (1) ascender stem, (2) arm-into-leg (upper-right → into stem → lower-right).
    private fun k() = LetterGuide(
        'k',
        false,
        listOf(
            stroke("M 30 15 L 30 82", Offset(30f, 17f), 0),
            stroke("M 66 44 L 34 67 L 68 82", Offset(64f, 45f), 1),
        ),
    )

    // 1 stroke: tall vertical.
    private fun l() = LetterGuide(
        'l',
        false,
        listOf(
            stroke("M 50 15 L 50 82", Offset(50f, 17f), 0),
        ),
    )

    // 3 strokes: leg 1, then hump+leg 2 (from upper leg 1), then hump+leg 3 (from upper leg 2).
    private fun m() = LetterGuide(
        'm',
        false,
        listOf(
            stroke("M 24 48 L 24 82", Offset(24f, 50f), 0),
            stroke("M 24 54 C 27 40 46 40 49 57 L 49 82", Offset(26f, 55f), 1),
            stroke("M 49 54 C 52 40 71 40 74 57 L 74 82", Offset(51f, 55f), 2),
        ),
    )

    // 2 strokes: (1) leg, (2) arch (from upper leg over to the right, down).
    private fun n() = LetterGuide(
        'n',
        false,
        listOf(
            stroke("M 30 48 L 30 82", Offset(30f, 50f), 0),
            stroke("M 30 54 C 34 40 65 40 69 57 L 69 82", Offset(32f, 55f), 1),
        ),
    )

    // 1 stroke: full circle, start top, counter-clockwise.
    private fun o() = LetterGuide(
        'o',
        false,
        listOf(
            stroke("M 50 41 C 30 41 30 82 50 82 C 70 82 70 41 50 41", Offset(50f, 42f), 0),
        ),
    )

    // 2 strokes: (1) stem into the descender, (2) bowl on the right (CW), staying above baseline.
    private fun p() = LetterGuide(
        'p',
        false,
        listOf(
            stroke("M 30 48 L 30 106", Offset(30f, 50f), 0),
            stroke("M 30 53 C 46 40 72 44 72 65 C 72 84 46 85 30 78", Offset(32f, 51f), 1),
        ),
    )

    // 2 strokes: (1) "c"-bowl, (2) right stem into the descender with a small tail flick right.
    private fun q() = LetterGuide(
        'q',
        false,
        listOf(
            stroke(
                "M 67 48 C 55 40 40 40 33 50 C 26 60 27 74 37 80 C 48 84 60 81 67 74",
                Offset(67f, 52f),
                0,
            ),
            stroke("M 67 52 L 67 98 C 67 98 77 97 83 91", Offset(67f, 53f), 1),
        ),
    )

    // 2 strokes: (1) stem, (2) small shoulder from the upper stem to the right.
    private fun r() = LetterGuide(
        'r',
        false,
        listOf(
            stroke("M 30 43 L 30 82", Offset(30f, 45f), 0),
            stroke("M 30 54 C 36 40 52 40 64 54", Offset(32f, 55f), 1),
        ),
    )

    // 1 stroke: S-curve, start top-right, ends bottom-left.
    private fun s() = LetterGuide(
        's',
        false,
        listOf(
            stroke(
                "M 70 52 C 56 42 33 44 34 56 C 35 66 67 64 67 76 C 67 88 42 89 29 77",
                Offset(69f, 55f),
                0,
            ),
        ),
    )

    // 2 strokes: (1) stem (taller than x-height, small bottom hook), (2) crossbar at midline.
    private fun t() = LetterGuide(
        't',
        false,
        listOf(
            stroke("M 44 28 L 44 76 C 44 82 55 83 66 78", Offset(44f, 30f), 0),
            stroke("M 28 42 L 60 42", Offset(31f, 37f), 1),
        ),
    )

    // 1 stroke: down the left, curve across the bottom, up the right (drawn once — NO retrace).
    private fun u() = LetterGuide(
        'u',
        false,
        listOf(
            stroke("M 31 44 L 31 76 C 31 84 56 84 59 76 L 59 44", Offset(31f, 46f), 0),
        ),
    )

    // 1 stroke: down to the point, up to top-right.
    private fun v() = LetterGuide(
        'v',
        false,
        listOf(
            stroke("M 29 44 L 50 82 L 71 44", Offset(29f, 46f), 0),
        ),
    )

    // 1 stroke: continuous zigzag.
    private fun w() = LetterGuide(
        'w',
        false,
        listOf(
            stroke("M 20 44 L 32 82 L 50 55 L 68 82 L 80 44", Offset(20f, 46f), 0),
        ),
    )

    // 2 strokes: two diagonals.
    private fun x() = LetterGuide(
        'x',
        false,
        listOf(
            stroke("M 29 44 L 71 82", Offset(29f, 46f), 0),
            stroke("M 71 44 L 29 82", Offset(71f, 46f), 1),
        ),
    )

    // 2 strokes: (1) short arm top-left → the junction AT the baseline; (2) long arm top-right →
    // the same baseline junction → then the tail continues down into the descender.
    private fun y() = LetterGuide(
        'y',
        false,
        listOf(
            stroke("M 31 48 L 47 88", Offset(31f, 50f), 0),
            stroke("M 71 48 L 47 88 L 33 106", Offset(71f, 50f), 1),
        ),
    )

    // 1 stroke: top across, diagonal down-left, bottom across.
    private fun z() = LetterGuide(
        'z',
        false,
        listOf(
            stroke("M 29 44 L 71 44 L 32 82 L 72 82", Offset(29f, 43f), 0),
        ),
    )
}
