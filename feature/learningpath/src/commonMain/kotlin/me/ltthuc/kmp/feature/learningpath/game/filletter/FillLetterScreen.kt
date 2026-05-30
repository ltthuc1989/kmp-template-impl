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
import me.ltthuc.kmp.core.resource.fill_letter_celebration_subtitle
import me.ltthuc.kmp.core.resource.fill_letter_celebration_title
import me.ltthuc.kmp.core.resource.fill_letter_guide
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.common.CreamBackground
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.LetterCircle
import me.ltthuc.kmp.feature.learningpath.game.filletter.view.WordWithBlank
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.PicturePanel
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
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
                    guideText = stringResource(Res.string.fill_letter_guide),
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
                            LetterCircle(
                                letter = letter,
                                tint = round.tint,
                                enabled = !ui.isResolving && !ui.isComplete,
                                shakeKey = if (isWrongPick) ui.currentRoundIndex * 100 + idx + 1 else 0,
                                onClick = { viewModel.onLetterTapped(letter) },
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
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
private fun buildOverlayFeedback(ui: FillLetterUiState): ScoreFeedback? = if (ui.isComplete) {
    ScoreFeedback.Success(
        title = stringResource(Res.string.fill_letter_celebration_title),
        subtitle = stringResource(Res.string.fill_letter_celebration_subtitle),
        heroEmoji = "🎉",
        primaryLabel = stringResource(Res.string.chant_next),
    )
} else {
    null
}
