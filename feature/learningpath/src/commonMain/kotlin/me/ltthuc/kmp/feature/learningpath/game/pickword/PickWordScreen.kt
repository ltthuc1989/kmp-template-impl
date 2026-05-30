package me.ltthuc.kmp.feature.learningpath.game.pickword

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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.pick_word_celebration_subtitle
import me.ltthuc.kmp.core.resource.pick_word_celebration_title
import me.ltthuc.kmp.core.resource.pick_word_guide
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.common.CreamBackground
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.AnswerSlot
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.PickWordChoice
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.PicturePanel
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Pick the word that matches the picture.
 *
 * 4 rounds; per round: hero emoji + answer slot + 2 word options. Tap correct = slot
 * fills + correct SFX, advance after [PickWordViewModel.CORRECT_HOLD_MS]; tap wrong =
 * the tapped option shakes via [PickWordChoice.shakeKey].
 */
@Composable
internal fun PickWordScreen(
    unitId: String,
    gameIndex: Int,
    totalGames: Int,
    onClose: () -> Unit,
    onJumpToGame: (Int) -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PickWordViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
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
                    onNext = onGameComplete,
                    nextEnabled = ui.isComplete,
                    guideText = stringResource(Res.string.pick_word_guide),
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CreamBackground),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RoundIndicator(
                        current = ui.currentRoundIndex + 1,
                        total = ui.totalRounds,
                    )
                    Spacer(Modifier.height(16.dp))

                    val round = ui.currentRound
                    PicturePanel(
                        emoji = round.targetEmoji,
                        modifier = Modifier
                            .fillMaxWidth(fraction = 0.55f)
                            .aspectRatio(1f),
                    )
                    Spacer(Modifier.height(20.dp))
                    AnswerSlot(
                        filledWord = if (ui.isResolving) round.targetWord else null,
                        modifier = Modifier.size(width = 220.dp, height = 64.dp),
                    )
                    Spacer(Modifier.weight(1f, fill = true))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                    ) {
                        round.choices.forEachIndexed { idx, word ->
                            val tint = if (idx == 0) round.tint else round.tint.copy(alpha = 0.75f)
                            // shakeKey changes when wrong pick equals this word — triggers shake.
                            val isWrongPick = ui.lastWrongPick == word
                            PickWordChoice(
                                word = word,
                                tint = tint,
                                enabled = !ui.isResolving && !ui.isComplete,
                                shakeKey = if (isWrongPick) ui.currentRoundIndex * 100 + idx + 1 else 0,
                                onClick = { viewModel.onWordTapped(word) },
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
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
private fun RoundIndicator(current: Int, total: Int) {
    Text(
        text = "$current / $total",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = ReadingTextDark.copy(alpha = 0.7f),
    )
}

@Composable
private fun buildOverlayFeedback(ui: PickWordUiState): ScoreFeedback? = if (ui.isComplete) {
    ScoreFeedback.Success(
        title = stringResource(Res.string.pick_word_celebration_title),
        subtitle = stringResource(Res.string.pick_word_celebration_subtitle),
        heroEmoji = "🎉",
        primaryLabel = stringResource(Res.string.chant_next),
    )
} else {
    null
}
