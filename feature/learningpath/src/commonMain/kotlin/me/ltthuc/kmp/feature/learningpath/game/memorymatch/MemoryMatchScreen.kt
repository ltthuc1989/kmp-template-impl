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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.memory_match_guide
import me.ltthuc.kmp.core.ui.audio.ScreenVoicePrompt
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.OceanDecorations
import me.ltthuc.kmp.feature.learningpath.game.common.GUIDE_IDLE_MS
import me.ltthuc.kmp.feature.learningpath.game.common.GameHandGuide
import me.ltthuc.kmp.feature.learningpath.game.common.HandStep
import me.ltthuc.kmp.feature.learningpath.game.common.OceanBackground
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.view.MemoryGrid
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

    ScreenVoicePrompt("vp_game_memory")

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
        containerColor = Color.Transparent,
    ) { ui ->
        // Finish → wait 1s → auto-advance to next game (no popup/effect).
        LaunchedEffect(ui.isComplete) {
            if (ui.isComplete) {
                delay(1_000L)
                onGameComplete()
            }
        }

        // Idle 5s → show the hand guide demoing a matching pair. Only BEFORE the first tap (teach the
        // mechanic once); after the kid starts, no more hand.
        val cardCenters = remember { mutableStateMapOf<Int, Offset>() }
        var boxOrigin by remember { mutableStateOf(Offset.Zero) }
        var interactionTick by remember { mutableStateOf(0) }
        var showHint by remember { mutableStateOf(false) }
        LaunchedEffect(interactionTick, ui.isComplete) {
            showHint = false
            if (!ui.isComplete && interactionTick == 0) {
                delay(GUIDE_IDLE_MS)
                showHint = true
            }
        }

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
                    .background(brush = OceanBackground)
                    .onGloballyPositioned { boxOrigin = it.boundsInWindow().topLeft },
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
                            onCardTap = { id ->
                                interactionTick++
                                viewModel.onCardTapped(id)
                            },
                            onCardPositioned = { id, center -> cardCenters[id] = center },
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                // Hand guide: demo the next unmatched matching pair (tap A → tap B).
                val pairIds = remember(ui.cards, ui.matchedIds) {
                    ui.cards.filter { it.id !in ui.matchedIds }
                        .groupBy { it.pairKey }.values
                        .firstOrNull { it.size >= 2 }?.take(2)?.map { it.id }
                }
                val taps = pairIds?.mapNotNull { cardCenters[it]?.minus(boxOrigin) } ?: emptyList()
                val steps = if (taps.size == 2) {
                    persistentListOf(HandStep.Tap(taps[0]), HandStep.Tap(taps[1]))
                } else {
                    persistentListOf()
                }
                GameHandGuide(isVisible = showHint && steps.isNotEmpty(), steps = steps)
            }
        }
    }
}
