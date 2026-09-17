package me.ltthuc.kmp.feature.learningpath.game.filletter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.DisposableEffect
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
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.ChunkChoice
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.WordWithBlank
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.choiceSpacing
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.PicturePanel
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Tap the missing chunk (letter, vowel, rime, cluster) to complete the word. Picture +
 * word-with-blank + 4 choices. 4 rounds. The word is spoken at the start of every round and
 * again when the picture is tapped.
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

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }
    val stepSegments = remember(totalGames) { gameSegmentsFor(totalGames) }

    // The round's word waits for the narrator — two voices at once is noise to a 4-year-old.
    // Flips immediately when voice is off, so a muted parent's child isn't left waiting.
    var guideDone by remember { mutableStateOf(false) }
    ScreenVoicePrompt("vp_game_fill") { guideDone = true }

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

        // Speak each round's word once it is on screen: a distractor often spells another real word
        // (`c_` + at = cat under a picture of a cap), and hearing "cap" is what settles it.
        LaunchedEffect(ui.currentRoundIndex, guideDone) {
            if (guideDone && !ui.isComplete) {
                delay(ROUND_WORD_DELAY_MS)
                viewModel.playRoundWord()
            }
        }

        // Idle 5s → hand guide points at the correct choice. Only on the FIRST round (teach once);
        // later rounds get no hand. A wrong tap re-arms it within round 0.
        val choiceCenters = remember { mutableStateMapOf<String, Offset>() }
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
                    val pictureTap = remember { MutableInteractionSource() }
                    PicturePanel(
                        word = round.picture,
                        modifier = Modifier
                            .fillMaxWidth(fraction = 0.45f)
                            .aspectRatio(1f)
                            .clickable(
                                interactionSource = pictureTap,
                                indication = null,
                                onClick = viewModel::playRoundWord,
                            ),
                    )
                    Spacer(Modifier.height(24.dp))
                    WordWithBlank(
                        word = round.fullWord,
                        blankSpans = round.blankSpans,
                        isFilled = ui.isResolving,
                    )
                    Spacer(Modifier.weight(1f, fill = true))

                    val longestLabel = round.choices.maxOf { it.length }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            choiceSpacing(longestLabel),
                            Alignment.CenterHorizontally,
                        ),
                    ) {
                        round.choices.forEachIndexed { idx, choice ->
                            val isWrongPick = ui.lastWrongPick == choice
                            Box(
                                modifier = Modifier.onGloballyPositioned {
                                    choiceCenters[choice] = it.boundsInWindow().center
                                },
                            ) {
                                ChunkChoice(
                                    label = choice,
                                    longestLabel = longestLabel,
                                    tint = round.tint,
                                    enabled = !ui.isResolving && !ui.isComplete,
                                    shakeKey = if (isWrongPick) ui.currentRoundIndex * 100 + idx + 1 else 0,
                                    onClick = {
                                        interactionTick++
                                        viewModel.onChoiceTapped(choice)
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                val targetCenter = choiceCenters[ui.currentRound.answer]
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

/** Beat between a new round appearing and its word being spoken. */
private const val ROUND_WORD_DELAY_MS = 400L
