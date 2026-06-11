package me.ltthuc.kmp.feature.learningpath.game.memorymatch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.memory_match_complete_subtitle
import me.ltthuc.kmp.core.resource.memory_match_complete_title
import me.ltthuc.kmp.core.resource.memory_match_guide
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.OceanDecorations
import me.ltthuc.kmp.feature.learningpath.game.common.OceanBackground
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.view.MemoryGrid
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Concentration / memory game — the second game in the post-Story flow.
 *
 * Same chrome contract as [me.ltthuc.kmp.feature.learningpath.game.bubblepop.BubblePopScreen]:
 * header shows game segment dots, bottom shows letter stepper, ocean background.
 */
@Composable
internal fun MemoryMatchScreen(
    unitId: String,
    gameIndex: Int,
    totalGames: Int,
    onClose: () -> Unit,
    onJumpToGame: (Int) -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryMatchViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val stepSegments = remember(totalGames) { gameSegmentsFor(totalGames) }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
        containerColor = Color.Transparent,
    ) { ui ->
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    currentStepIndex = gameIndex,
                    stepSegments = stepSegments,
                    onClose = onClose,
                    onStepJump = onJumpToGame,
                    guideText = stringResource(Res.string.memory_match_guide),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(brush = OceanBackground),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        OceanDecorations()
                        MemoryGrid(
                            cards = ui.cards,
                            matchedIds = ui.matchedIds,
                            selectedIds = ui.selectedIds,
                            enabled = !ui.isResolving && !ui.isComplete,
                            onCardTap = viewModel::onCardTapped,
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                ScoreFeedbackOverlay(
                    feedback = buildOverlayFeedback(ui),
                    onDismiss = { /* tap-outside disabled */ },
                    onPrimary = onGameComplete,
                )
            }
        }
    }
}

@Composable
private fun buildOverlayFeedback(ui: MemoryMatchUiState): ScoreFeedback? = if (ui.isComplete) {
    ScoreFeedback.Success(
        title = stringResource(Res.string.memory_match_complete_title),
        subtitle = stringResource(Res.string.memory_match_complete_subtitle),
        heroEmoji = "🎉",
        primaryLabel = stringResource(Res.string.chant_next),
    )
} else {
    null
}
