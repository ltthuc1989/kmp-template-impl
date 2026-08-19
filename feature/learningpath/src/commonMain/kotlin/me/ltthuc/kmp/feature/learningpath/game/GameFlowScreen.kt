package me.ltthuc.kmp.feature.learningpath.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitCompletionRepository
import me.ltthuc.kmp.core.ui.audio.rememberAudioSession
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.BubblePopScreen
import me.ltthuc.kmp.feature.learningpath.game.dragwords.DragWordsScreen
import me.ltthuc.kmp.feature.learningpath.game.filletter.FillLetterScreen
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.MemoryMatchScreen
import me.ltthuc.kmp.feature.learningpath.game.pickword.PickWordScreen
import me.ltthuc.kmp.feature.learningpath.game.spellletters.SpellLettersScreen
import me.ltthuc.kmp.feature.learningpath.step.common.ConfettiCanvas
import org.koin.compose.koinInject

/**
 * Router for the post-Story game flow. Routes by [gameIndex] into the concrete game screen
 * and owns shared concerns:
 *
 * - **Progress write** — single LaunchedEffect bumps LearningProgressEntity each time the
 *   user lands on a new game; sub-game progress (e.g. round 1/3 in BubblePop) is up to
 *   the game itself to refine if it wants finer granularity later.
 * - **Advance + finish** — `onGameComplete` callback decides next destination: next game
 *   if any, otherwise it celebrates in place (confetti + congrats audio), marks the unit
 *   completed, and auto-returns to the Lesson Map (or Level Complete for the last unit).
 *   There is no standalone Unit Complete screen.
 * - **Jump between games** — segment-row taps in the chrome route to UnitGame(targetIndex).
 * - **Close** — pops back-stack down to [Destination.Learning.UnitSelection] (mandatory
 *   completion: no shortcut to the finish celebration via Close).
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
    val unitCompletionRepository: UnitCompletionRepository = koinInject()
    val levelRepository: LevelRepository = koinInject()
    val congratsAudio = rememberAudioSession()
    val lang = LocalAppLanguage.current
    val games = DEFAULT_UNIT_GAMES
    val totalGames = games.size
    val clampedIndex = gameIndex.coerceIn(0, totalGames - 1)
    val currentGame = games[clampedIndex]

    // Collected up front so it's loaded by the time the last game finishes (the flow runs for
    // minutes). null = no next unit = last unit of the level → celebrate the whole level.
    val nextUnit by levelRepository.observeNextUnit(unitId)
        .collectAsStateWithLifecycle(initialValue = null)

    // Set when the final game is cleared: drives the in-place confetti + congrats audio before
    // we auto-leave. Replaces the old standalone UnitComplete screen.
    var finishing by remember { mutableStateOf(false) }

    LaunchedEffect(finishing) {
        if (!finishing) return@LaunchedEffect
        // Mark the unit done first so the next unit unlocks even if the user leaves mid-audio.
        unitCompletionRepository.markCompleted(unitId)
        // No finish chime — go straight to the congrats voice prompt below.
        delay(CELEBRATE_PRAISE_DELAY_MS)
        congratsAudio.playAndAwait(AudioRef.Prompt("vp_lesson_done", lang), CONGRATS_MAX_MS)
        if (nextUnit == null) {
            // Last unit of the level → whole-level celebration.
            navBackStack.add(Destination.Learning.LevelComplete(levelId = levelId))
        } else {
            // Pop the accumulated Lesson Map + Story + game screens back to the unit list.
            while (
                navBackStack.size > 1 &&
                navBackStack.last() !is Destination.Learning.UnitSelection
            ) {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
            if (navBackStack.lastOrNull() !is Destination.Learning.UnitSelection) {
                navBackStack.add(Destination.Learning.UnitSelection(levelId))
            }
        }
    }

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
            // All games done → celebrate in place (confetti + congrats audio) then auto-leave.
            Napier.d(tag = TAG) { "Game flow finished for $levelId/$unitId → celebrate + exit" }
            finishing = true
        } else {
            Napier.d(tag = TAG) { "Game $clampedIndex done → UnitGame($nextIndex)" }
            navBackStack.add(
                Destination.Learning.UnitGame(levelId, unitId, gameIndex = nextIndex),
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (currentGame) {
            UnitGame.BUBBLE_POP -> BubblePopScreen(
                unitId = unitId,
                gameIndex = clampedIndex,
                totalGames = totalGames,
                onClose = onClose,
                onJumpToGame = onJumpToGame,
                onGameComplete = onGameComplete,
                modifier = Modifier.fillMaxSize(),
            )
            UnitGame.MEMORY_MATCH -> MemoryMatchScreen(
                unitId = unitId,
                gameIndex = clampedIndex,
                totalGames = totalGames,
                onClose = onClose,
                onJumpToGame = onJumpToGame,
                onGameComplete = onGameComplete,
                modifier = Modifier.fillMaxSize(),
            )
            UnitGame.PICK_WORD -> PickWordScreen(
                unitId = unitId,
                gameIndex = clampedIndex,
                totalGames = totalGames,
                onClose = onClose,
                onJumpToGame = onJumpToGame,
                onGameComplete = onGameComplete,
                modifier = Modifier.fillMaxSize(),
            )
            UnitGame.FILL_LETTER -> FillLetterScreen(
                unitId = unitId,
                gameIndex = clampedIndex,
                totalGames = totalGames,
                onClose = onClose,
                onJumpToGame = onJumpToGame,
                onGameComplete = onGameComplete,
                modifier = Modifier.fillMaxSize(),
            )
            UnitGame.SPELL_LETTERS -> SpellLettersScreen(
                unitId = unitId,
                gameIndex = clampedIndex,
                totalGames = totalGames,
                onClose = onClose,
                onJumpToGame = onJumpToGame,
                onGameComplete = onGameComplete,
                modifier = Modifier.fillMaxSize(),
            )
            UnitGame.DRAG_WORDS -> DragWordsScreen(
                unitId = unitId,
                gameIndex = clampedIndex,
                totalGames = totalGames,
                onClose = onClose,
                onJumpToGame = onJumpToGame,
                onGameComplete = onGameComplete,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Confetti rain over the game while the congrats audio plays, before we auto-leave.
        if (finishing) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())
        }
    }
}

// Small beat after the fanfare chime before the spoken congrats, mirroring TracingScreen.
private const val CELEBRATE_PRAISE_DELAY_MS = 600L
private const val CONGRATS_MAX_MS = 6_000L
private const val TAG = "GameFlowScreen"
