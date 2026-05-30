package me.ltthuc.kmp.feature.learningpath.game

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import io.github.aakira.napier.Napier
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.BubblePopScreen
import me.ltthuc.kmp.feature.learningpath.game.dragwords.DragWordsScreen
import me.ltthuc.kmp.feature.learningpath.game.filletter.FillLetterScreen
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.MemoryMatchScreen
import me.ltthuc.kmp.feature.learningpath.game.pickword.PickWordScreen
import me.ltthuc.kmp.feature.learningpath.game.spellletters.SpellLettersScreen
import org.koin.compose.koinInject

/**
 * Router for the post-Story game flow. Routes by [gameIndex] into the concrete game screen
 * and owns shared concerns:
 *
 * - **Progress write** — single LaunchedEffect bumps LearningProgressEntity each time the
 *   user lands on a new game; sub-game progress (e.g. round 1/3 in BubblePop) is up to
 *   the game itself to refine if it wants finer granularity later.
 * - **Advance + finish** — `onGameComplete` callback decides next destination: next game
 *   if any, otherwise [Destination.Learning.UnitComplete].
 * - **Jump between games** — segment-row taps in the chrome route to UnitGame(targetIndex).
 * - **Close** — pops back-stack down to [Destination.Learning.UnitSelection] (mandatory
 *   completion: no shortcut to UnitComplete via Close).
 *
 * Each game implementation should accept [gameIndex] + [DEFAULT_UNIT_GAMES] size so its
 * header renders the right segment dot highlighted.
 */
@Composable
internal fun GameFlowScreen(
    levelId: String,
    unitId: String,
    gameIndex: Int,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val progressRepository: LearningProgressRepository = koinInject()
    val games = DEFAULT_UNIT_GAMES
    val totalGames = games.size
    val clampedIndex = gameIndex.coerceIn(0, totalGames - 1)
    val currentGame = games[clampedIndex]

    LaunchedEffect(levelId, unitId, clampedIndex) {
        val pct = ((clampedIndex.toFloat() / totalGames.coerceAtLeast(1)) * 100).toInt()
            .coerceIn(0, 99)
        progressRepository.setActivePosition(
            levelId = levelId,
            unitId = unitId,
            lessonIndex = 0, // game flow is unit-level — no per-lesson context
            stepIndex = GAME_FLOW_STEP_INDEX,
            progressPercent = pct,
        )
    }

    val onClose: () -> Unit = {
        // Mandatory completion: Close exits to UnitSelection (kid loses stars + must replay).
        val bookIdx = navBackStack.indexOfLast { it is Destination.Learning.UnitSelection }
        if (bookIdx >= 0) {
            while (navBackStack.size > bookIdx + 1) navBackStack.removeAt(navBackStack.lastIndex)
        } else if (navBackStack.size > 1) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
    }

    val onJumpToGame: (Int) -> Unit = { targetIndex ->
        if (targetIndex != clampedIndex && targetIndex in 0 until totalGames) {
            // Replace current entry so back-stack doesn't bloat with every segment tap.
            if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
            navBackStack.add(Destination.Learning.UnitGame(levelId, unitId, gameIndex = targetIndex))
        }
    }

    val onGameComplete: () -> Unit = {
        val nextIndex = clampedIndex + 1
        if (nextIndex >= totalGames) {
            // All games done → mark step 100% then advance to UnitComplete celebration.
            Napier.d(tag = TAG) { "Game flow finished for $levelId/$unitId → UnitComplete" }
            navBackStack.add(
                Destination.Learning.UnitComplete(
                    levelId = levelId,
                    unitId = unitId,
                    starsEarned = UNIT_COMPLETE_STARS,
                ),
            )
        } else {
            Napier.d(tag = TAG) { "Game $clampedIndex done → UnitGame($nextIndex)" }
            navBackStack.add(
                Destination.Learning.UnitGame(levelId, unitId, gameIndex = nextIndex),
            )
        }
    }

    when (currentGame) {
        UnitGame.BUBBLE_POP -> BubblePopScreen(
            unitId = unitId,
            gameIndex = clampedIndex,
            totalGames = totalGames,
            onClose = onClose,
            onJumpToGame = onJumpToGame,
            onGameComplete = onGameComplete,
            modifier = modifier.fillMaxSize(),
        )
        UnitGame.MEMORY_MATCH -> MemoryMatchScreen(
            unitId = unitId,
            gameIndex = clampedIndex,
            totalGames = totalGames,
            onClose = onClose,
            onJumpToGame = onJumpToGame,
            onGameComplete = onGameComplete,
            modifier = modifier.fillMaxSize(),
        )
        UnitGame.PICK_WORD -> PickWordScreen(
            unitId = unitId,
            gameIndex = clampedIndex,
            totalGames = totalGames,
            onClose = onClose,
            onJumpToGame = onJumpToGame,
            onGameComplete = onGameComplete,
            modifier = modifier.fillMaxSize(),
        )
        UnitGame.FILL_LETTER -> FillLetterScreen(
            unitId = unitId,
            gameIndex = clampedIndex,
            totalGames = totalGames,
            onClose = onClose,
            onJumpToGame = onJumpToGame,
            onGameComplete = onGameComplete,
            modifier = modifier.fillMaxSize(),
        )
        UnitGame.SPELL_LETTERS -> SpellLettersScreen(
            unitId = unitId,
            gameIndex = clampedIndex,
            totalGames = totalGames,
            onClose = onClose,
            onJumpToGame = onJumpToGame,
            onGameComplete = onGameComplete,
            modifier = modifier.fillMaxSize(),
        )
        UnitGame.DRAG_WORDS -> DragWordsScreen(
            unitId = unitId,
            gameIndex = clampedIndex,
            totalGames = totalGames,
            onClose = onClose,
            onJumpToGame = onJumpToGame,
            onGameComplete = onGameComplete,
            modifier = modifier.fillMaxSize(),
        )
    }
}

private const val UNIT_COMPLETE_STARS = 24
private const val TAG = "GameFlowScreen"
