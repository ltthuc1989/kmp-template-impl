package me.ltthuc.kmp.feature.learningpath.game.filletter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.fill_letter_guide
import me.ltthuc.kmp.core.ui.audio.ScreenVoicePrompt
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.common.CreamBackground
import me.ltthuc.kmp.feature.learningpath.game.common.GUIDE_IDLE_MS
import me.ltthuc.kmp.feature.learningpath.game.common.GameHandGuide
import me.ltthuc.kmp.feature.learningpath.game.common.HandStep
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.LetterCircle
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.WordWithBlank
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.PicturePanel
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Tap the missing letter to complete the word. Picture + word-with-blank + 2 letter circles.
 * 4 rounds.
 */
@Composable
internal fun FillLetterScreen(
    unitId: String,
    gameIndex: Int,
    totalGames: Int,
    onClose: () -> Unit,
    onJumpToGame: (Int) -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FillLetterViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val stepSegments = remember(totalGames) { gameSegmentsFor(totalGames) }

    ScreenVoicePrompt("vp_game_fill")

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
        containerColor = Color.Transparent,
    ) { ui ->
        // Auto-advance to the next game once the final round's word audio has finished
        // (isComplete is only set after playWordAndAwait completes in the ViewModel).
        LaunchedEffect(ui.isComplete) {
            if (ui.isComplete) {
                delay(1_000L)
                onGameComplete()
            }
        }

        // Idle 5s → hand guide points at the correct letter. Only on the FIRST round (teach once);
        // later rounds get no hand. A wrong tap re-arms it within round 0.
        val letterCenters = remember { mutableStateMapOf<Char, Offset>() }
        var boxOrigin by remember { mutableStateOf(Offset.Zero) }
        var interactionTick by remember { mutableStateOf(0) }
        var showHint by remember { mutableStateOf(false) }
        LaunchedEffect(interactionTick, ui.currentRoundIndex, ui.isComplete) {
            showHint = false
            if (!ui.isComplete && ui.currentRoundIndex == 0) {
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
                    guideText = stringResource(Res.string.fill_letter_guide),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CreamBackground)
                    .onGloballyPositioned { boxOrigin = it.boundsInWindow().topLeft },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${ui.currentRoundIndex + 1} / ${ui.totalRounds}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ReadingTextDark.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(12.dp))

                    val round = ui.currentRound
                    PicturePanel(
                        emoji = round.emoji,
                        modifier = Modifier
                            .fillMaxWidth(fraction = 0.45f)
                            .aspectRatio(1f),
                    )
                    Spacer(Modifier.height(24.dp))
                    WordWithBlank(
                        word = round.fullWord,
                        blankIndex = round.blankIndex,
                        isFilled = ui.isResolving,
                    )
                    Spacer(Modifier.weight(1f, fill = true))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                    ) {
                        round.choices.forEachIndexed { idx, letter ->
                            val isWrongPick = ui.lastWrongPick == letter
                            Box(
                                modifier = Modifier.onGloballyPositioned {
                                    letterCenters[letter] = it.boundsInWindow().center
                                },
                            ) {
                                LetterCircle(
                                    letter = letter,
                                    tint = round.tint,
                                    enabled = !ui.isResolving && !ui.isComplete,
                                    shakeKey = if (isWrongPick) ui.currentRoundIndex * 100 + idx + 1 else 0,
                                    onClick = {
                                        interactionTick++
                                        viewModel.onLetterTapped(letter)
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                val correct = ui.currentRound.correctLetter
                val targetCenter = letterCenters.entries
                    .firstOrNull { it.key.equals(correct, ignoreCase = true) }?.value
                val steps = targetCenter
                    ?.let { persistentListOf(HandStep.Tap(it - boxOrigin)) }
                    ?: persistentListOf()
                GameHandGuide(
                    isVisible = showHint && !ui.isResolving && steps.isNotEmpty(),
                    steps = steps,
                )
            }
        }
    }
}
