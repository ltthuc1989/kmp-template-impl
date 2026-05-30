package me.ltthuc.kmp.feature.learningpath.game

/**
 * Catalogue of the mini-games that play in the post-Story game flow.
 *
 * Order matters: the index of an entry in [DEFAULT_UNIT_GAMES] is the `gameIndex` value
 * passed via the [me.ltthuc.kmp.core.ui.screen.Destination.Learning.UnitGame] destination.
 * Adding a new game = adding a new enum entry, appending it here, and registering a router
 * branch in [GameFlowScreen].
 *
 * V1.x roadmap (not yet built): SoundMatch (phoneme → letter), WhackLetter (timed taps).
 */
internal enum class UnitGame {
    BUBBLE_POP,
    MEMORY_MATCH,
    PICK_WORD,
    FILL_LETTER,
    SPELL_LETTERS,
    DRAG_WORDS,
}

internal val DEFAULT_UNIT_GAMES: List<UnitGame> = listOf(
    UnitGame.BUBBLE_POP,
    UnitGame.MEMORY_MATCH,
    UnitGame.FILL_LETTER,
    UnitGame.PICK_WORD,
    UnitGame.SPELL_LETTERS,
    UnitGame.DRAG_WORDS,
)

/**
 * Sentinel `stepIndex` value written to LearningProgressEntity while the user is anywhere
 * inside the game flow. Step indices 0..6 are canonical lessons and 7 is Story; 8 means
 * "past Story, in game flow". Sub-position inside game flow is captured by the
 * `progressPercent` field, not a finer-grained stepIndex.
 */
internal const val GAME_FLOW_STEP_INDEX = 8
