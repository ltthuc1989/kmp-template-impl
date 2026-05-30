package me.ltthuc.kmp.feature.learningpath.game.bubblepop

import kotlin.random.Random

/**
 * Specifies one bubble in a round. `id` is stable for the lifetime of a round so the
 * physics layer can track each bubble's position by id (new round → new ids → fresh spawn).
 *
 * `isTarget` lets the canvas mark correct vs distractor without re-comparing letters.
 */
internal data class BubbleSpec(
    val id: Int,
    val letter: String,
    val isTarget: Boolean,
)

/**
 * Builds a round's bubble list given the target letter and the unit's other letters.
 *
 * Mix: [TARGET_COUNT] target bubbles + [DISTRACTOR_COUNT] distractors. Distractors come
 * preferentially from the unit's other letters (so kids see what they've recently learned),
 * then padded with random letters from the rest of the alphabet — matches the screenshot
 * where bubbles span A/B/E across the strip rather than 3 adjacent letters only.
 *
 * Deterministic when [random] is seeded — useful for tests + the dev preview screen.
 */
internal fun spawnBubblesForRound(
    targetLetter: String,
    unitLetters: List<String>,
    random: Random = Random.Default,
): List<BubbleSpec> {
    val target = targetLetter.uppercase()
    val unitDistractors = unitLetters.map { it.uppercase() }.filter { it != target }.distinct()
    val alphabetPool = ('A'..'Z').map { it.toString() }
        .filter { it != target && it !in unitDistractors }
        .shuffled(random)

    val distractorLetters = buildList {
        addAll(unitDistractors.shuffled(random).take(UNIT_DISTRACTOR_MAX))
        val needed = DISTRACTOR_COUNT - size
        if (needed > 0) addAll(alphabetPool.take(needed))
    }.take(DISTRACTOR_COUNT)

    // 10 target spawns pre-generated, alternating upper/lower case → 5 "A" + 5 "a".
    // Canvas staggers their initial Y positions so all 10 enter visible within ~20s.
    val targetCaseForms = listOf(target.uppercase(), target.lowercase())
    val targets = List(TARGET_COUNT) { idx ->
        BubbleSpec(
            id = idx,
            letter = targetCaseForms[idx % targetCaseForms.size],
            isTarget = true,
        )
    }
    val distractors = distractorLetters.mapIndexed { i, letter ->
        // Random case per distractor for visual variety on the screen.
        val displayLetter = if (random.nextBoolean()) letter.uppercase() else letter.lowercase()
        BubbleSpec(id = TARGET_COUNT + i, letter = displayLetter, isTarget = false)
    }
    return (targets + distractors).shuffled(random)
}

/** Random upper/lower case form of [letter]. */
internal fun randomCase(letter: String, random: Random = Random.Default): String =
    if (random.nextBoolean()) letter.uppercase() else letter.lowercase()

// Total target spawns per round across upper + lower cases (5 + 5 = 10).
// All pre-spawned at round start with staggered Y, no respawn after pop/escape.
internal const val TARGET_COUNT = 10
internal const val DISTRACTOR_COUNT = 4
internal const val UNIT_DISTRACTOR_MAX = 1
internal const val BUBBLE_COUNT = TARGET_COUNT + DISTRACTOR_COUNT

/**
 * Total target letter bubbles spawned per round across upper + lower cases. After this many
 * targets have appeared on screen, no more target spawns — kid sees only distractors until
 * the round timer expires. Default 10 per user spec v5c. Will be configurable via settings later.
 */
internal const val BUBBLE_POP_TARGET_POOL = 10
