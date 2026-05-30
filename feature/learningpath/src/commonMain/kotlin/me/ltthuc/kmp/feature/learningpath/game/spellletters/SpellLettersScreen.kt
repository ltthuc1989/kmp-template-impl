package me.ltthuc.kmp.feature.learningpath.game.spellletters

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.spell_letters_celebration_subtitle
import me.ltthuc.kmp.core.resource.spell_letters_celebration_title
import me.ltthuc.kmp.core.resource.spell_letters_guide
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.common.CreamBackground
import me.ltthuc.kmp.feature.learningpath.game.common.ReadingTextDark
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.pickword.view.PicturePanel
import me.ltthuc.kmp.feature.learningpath.game.spellletters.view.DraggableLetterTile
import me.ltthuc.kmp.feature.learningpath.game.spellletters.view.WordSlot
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.hypot

/**
 * Drag letter tiles from the scatter row into the target word's slots. Industry-standard:
 * wrong drop springs back, correct drop locks. No bottom bar, cream background.
 */
@Composable
internal fun SpellLettersScreen(
    unitId: String,
    gameIndex: Int,
    totalGames: Int,
    onClose: () -> Unit,
    onJumpToGame: (Int) -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SpellLettersViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
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
                    guideText = stringResource(Res.string.spell_letters_guide),
                )
            },
        ) { innerPadding ->
            SpellLettersCanvas(
                ui = ui,
                onGameComplete = onGameComplete,
                onDrop = viewModel::onLetterDroppedOnSlot,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CreamBackground),
            )
        }
    }
}

@Composable
private fun SpellLettersCanvas(
    ui: SpellLettersUiState,
    onGameComplete: () -> Unit,
    onDrop: (letter: Char, slotIndex: Int?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val snapRadiusPx = with(density) { SNAP_RADIUS_DP.dp.toPx() }

    val slotCenters = remember { mutableStateMapOf<Int, Offset>() }
    val matchedSnapTargets = remember { mutableStateMapOf<Char, Offset>() }

    // On every round transition, wipe drag/snap state so the next word's tiles don't
    // inherit positions/snap targets from the previous word (which made them auto-fly to
    // remembered slot centers instead of resetting at the bottom scatter row).
    androidx.compose.runtime.LaunchedEffect(ui.currentRoundIndex) {
        slotCenters.clear()
        matchedSnapTargets.clear()
    }

    Box(modifier = modifier) {
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
                    .fillMaxWidth(fraction = 0.40f)
                    .aspectRatio(1f),
            )
            Spacer(Modifier.height(20.dp))

            // Key both rows by current round so each new round forces fresh internal state
            // in WordSlot + DraggableLetterTile (Animatable position resets to scatter origin).
            androidx.compose.runtime.key(ui.currentRoundIndex) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    round.word.forEachIndexed { slotIdx, char ->
                        WordSlot(
                            letter = char,
                            filled = slotIdx in ui.filledSlots,
                            onCenterPositioned = { slotCenters[slotIdx] = it },
                        )
                    }
                }

                Spacer(Modifier.weight(1f, fill = true))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                ) {
                    round.tileOrder.forEach { letter ->
                        val slotIdx = round.word.indexOfFirst { it.equals(letter, ignoreCase = true) }
                        val isUsed = slotIdx in ui.filledSlots
                        DraggableLetterTile(
                            letter = letter,
                            tint = round.tint,
                            isUsed = isUsed,
                            snapTarget = matchedSnapTargets[letter],
                            onCenterPositioned = { /* origin captured internally */ },
                            onDragEnd = { center ->
                                val nearest = nearestSlot(center, slotCenters, snapRadiusPx)
                                val matched = onDrop(letter, nearest)
                                if (matched && nearest != null) {
                                    slotCenters[nearest]?.let { slot ->
                                        matchedSnapTargets[letter] = slot
                                    }
                                }
                                matched
                            },
                        )
                    }
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

private fun nearestSlot(
    tileCenter: Offset,
    slotCenters: Map<Int, Offset>,
    radiusPx: Float,
): Int? {
    return slotCenters
        .map { (idx, center) -> idx to hypot(center.x - tileCenter.x, center.y - tileCenter.y) }
        .filter { (_, dist) -> dist <= radiusPx }
        .minByOrNull { it.second }
        ?.first
}

@Composable
private fun buildOverlayFeedback(ui: SpellLettersUiState): ScoreFeedback? = if (ui.isComplete) {
    ScoreFeedback.Success(
        title = stringResource(Res.string.spell_letters_celebration_title),
        subtitle = stringResource(Res.string.spell_letters_celebration_subtitle),
        heroEmoji = "🎉",
        primaryLabel = stringResource(Res.string.chant_next),
    )
} else {
    null
}

private const val SNAP_RADIUS_DP = 80
