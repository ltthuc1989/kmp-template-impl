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

/**
 * Vòng chơi của Level 2+: mục tiêu là một VẦN ("am", "ip"), không phải chữ cái.
 *
 * Ba điểm khác [spawnBubblesForRound], đều do user chốt 2026-08-12:
 *
 * 1. VẦN nhiễu chỉ được lấy trong chính unit đó — unit 2 không được thấy "ip"
 *    hay "ug" của unit khác, vì bé chưa học. Thiếu thì độn bằng CHỮ CÁI A-Z:
 *    chữ cái đã học ở Level 1 nên không phải nội dung lạ, và nhờ đó unit ít vần
 *    vẫn đủ 4 bong bóng nhiễu mà không phải lặp lại cùng một vần.
 * 2. Toàn chữ THƯỜNG. Level 1 xen hoa/thường để bé nhận mặt chữ (A và a là hai
 *    hình của một chữ); phonics dạy vần bằng chữ thường nên không có việc đó.
 * 3. Vẫn đúng [TARGET_COUNT] bong bóng mục tiêu mỗi vòng — giống Level 1.
 */
internal fun spawnBubblesForRimeRound(
    targetRime: String,
    unitRimes: List<String>,
    random: Random = Random.Default,
): List<BubbleSpec> {
    val target = targetRime.lowercase()
    val others = unitRimes.map { it.lowercase() }.filter { it != target }.distinct()

    // Chữ cái độn phải khác mục tiêu: lesson nguyên âm đơn có vần đúng một ký tự
    // ("a"), để lọt thì bong bóng nhiễu trùng hệt mục tiêu.
    val letterPool = ('a'..'z').map { it.toString() }
        .filter { it != target && it !in others }
        .shuffled(random)

    val distractorRimes = buildList {
        addAll(others.shuffled(random).take(DISTRACTOR_COUNT))
        val needed = DISTRACTOR_COUNT - size
        if (needed > 0) addAll(letterPool.take(needed))
    }.take(DISTRACTOR_COUNT)

    val targets = List(TARGET_COUNT) { idx -> BubbleSpec(id = idx, letter = target, isTarget = true) }
    val distractors = distractorRimes.mapIndexed { i, rime ->
        BubbleSpec(id = TARGET_COUNT + i, letter = rime, isTarget = false)
    }
    return (targets + distractors).shuffled(random)
}

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
