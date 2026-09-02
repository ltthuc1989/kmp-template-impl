package me.ltthuc.kmp.feature.learningpath.game.bubblepop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.bubble_game_lightning_fast
import me.ltthuc.kmp.core.resource.bubble_game_round_score
import me.ltthuc.kmp.core.resource.bubble_game_time_up
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.BubbleCanvas
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.CircularTimerWithCounter
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.OceanDecorations
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.TargetRimeCard
import me.ltthuc.kmp.feature.learningpath.game.common.OceanBackground
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Bubble Pop v5 — 30s arcade round, kid pops up to 10 target letter bubbles for tier stars.
 *
 * Chrome:
 * - StepHeader top: segments, close X, next →, guide text "Find the letter X"
 * - Top-left over canvas: [CircularTimerWithCounter] = 30s countdown + popped/10 counter
 * - No bottom bar (removed v5)
 * - Round-end overlay: tier stars (1-5) + score recap
 */
@Composable
internal fun BubblePopScreen(
    unitId: String,
    gameIndex: Int,
    totalGames: Int,
    onClose: () -> Unit,
    onJumpToGame: (Int) -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BubblePopViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val stepSegments = remember(totalGames) { gameSegmentsFor(totalGames) }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
        containerColor = Color.Transparent,
    ) { uiState ->
        BubblePopContent(
            ui = uiState,
            stepSegments = stepSegments,
            currentSegment = gameIndex,
            onClose = onClose,
            onStepJump = onJumpToGame,
            onBubbleTapped = viewModel::onBubbleTapped,
            onAdvance = viewModel::onAdvanceRound,
            onNext = onGameComplete,
        )
    }
}

@Composable
private fun BubblePopContent(
    ui: BubblePopUiState,
    stepSegments: ImmutableList<Int>,
    currentSegment: Int,
    onClose: () -> Unit,
    onStepJump: (Int) -> Unit,
    onBubbleTapped: (BubbleSpec, Boolean) -> Unit,
    onAdvance: () -> Unit,
    onNext: () -> Unit,
) {
    // Game finished → wait 1s → auto-advance to next game (no popup/effect).
    LaunchedEffect(ui.isGameComplete) {
        if (ui.isGameComplete) {
            delay(1_000L)
            onNext()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                currentStepIndex = currentSegment,
                stepSegments = stepSegments,
                onClose = onClose,
                onStepJump = onStepJump,
                showGuideText = false,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(brush = OceanBackground),
        ) {
            OceanDecorations()
            ui.revealedTarget?.let { rime ->
                TargetRimeCard(rime = rime, modifier = Modifier.align(Alignment.Center))
            }
            if (ui.bubbles.isNotEmpty() && !ui.isRoundComplete && !ui.isGameComplete && !ui.isGuidePlaying) {
                BubbleCanvas(
                    bubbles = ui.bubbles,
                    onBubbleTapped = onBubbleTapped,
                )
            }
            // Top-left circular timer + counter
            CircularTimerWithCounter(
                timeRemainingMs = ui.timeRemainingMs,
                totalMs = ui.roundDurationMs,
                popped = ui.popCount,
                targetPool = ui.targetPool,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp),
            )

            ScoreFeedbackOverlay(
                feedback = buildOverlayFeedback(ui),
                onDismiss = { /* tap-outside disabled — force primary action */ },
                onPrimary = onAdvance,
            )
        }
    }
}

@Composable
private fun buildOverlayFeedback(ui: BubblePopUiState): ScoreFeedback? = when {
    // Game finished → no popup (auto-advances after 1s). Only the mid-game round recap shows.
    ui.isRoundComplete && !ui.isGameComplete -> {
        val perfect = ui.popCount >= ui.targetPool
        val title = stringResource(
            if (perfect) Res.string.bubble_game_lightning_fast else Res.string.bubble_game_time_up,
        )
        ScoreFeedback.Success(
            title = title,
            subtitle = stringResource(Res.string.bubble_game_round_score, ui.popCount, ui.targetPool),
            primaryLabel = stringResource(Res.string.chant_next),
            starRating = ui.roundStars.coerceIn(0, 5),
        )
    }
    else -> null
}
