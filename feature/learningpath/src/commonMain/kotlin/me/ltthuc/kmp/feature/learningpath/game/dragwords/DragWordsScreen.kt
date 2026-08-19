package me.ltthuc.kmp.feature.learningpath.game.dragwords

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.drag_words_guide
import me.ltthuc.kmp.core.ui.audio.ScreenVoicePrompt
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.game.common.CreamBackground
import me.ltthuc.kmp.feature.learningpath.game.common.GUIDE_IDLE_MS
import me.ltthuc.kmp.feature.learningpath.game.common.GameHandGuide
import me.ltthuc.kmp.feature.learningpath.game.common.HandStep
import me.ltthuc.kmp.feature.learningpath.game.common.gameSegmentsFor
import me.ltthuc.kmp.feature.learningpath.game.dragwords.view.DraggableWord
import me.ltthuc.kmp.feature.learningpath.game.dragwords.view.PictureSlot
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.hypot

/**
 * Real drag-and-drop: kid grabs a word card and drops it on the matching picture.
 *
 * - Picture slots register their center positions (window coords) via callbacks.
 * - Word tiles also register origin centers, plus track drag delta in their own Animatable.
 * - On drag end, we compute the tile's absolute center, find the nearest picture slot
 *   within [SNAP_RADIUS_DP], and ask the ViewModel to score the drop. If correct, the
 *   tile snaps into that slot via [DraggableWord]'s `snapTarget` and locks (isUsed=true).
 *   If wrong, the tile springs back to origin and the picture shakes.
 *
 * 1 round, complete when all 4 are matched.
 */
@Composable
internal fun DragWordsScreen(
    unitId: String,
    gameIndex: Int,
    totalGames: Int,
    onClose: () -> Unit,
    onJumpToGame: (Int) -> Unit,
    onGameComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DragWordsViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }
    val stepSegments = remember(totalGames) { gameSegmentsFor(totalGames) }

    ScreenVoicePrompt("vp_game_drag")

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
                    guideText = stringResource(Res.string.drag_words_guide),
                )
            },
        ) { innerPadding ->
            DragWordsCanvas(
                ui = ui,
                onGameComplete = onGameComplete,
                onDrop = viewModel::onWordDroppedOnPicture,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CreamBackground),
            )
        }
    }
}

@Composable
private fun DragWordsCanvas(
    ui: DragWordsUiState,
    onGameComplete: () -> Unit,
    onDrop: (wordId: Int, pictureId: Int?) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val snapRadiusPx = with(density) { SNAP_RADIUS_DP.dp.toPx() }

    val pictureCenters = remember { mutableStateMapOf<Int, Offset>() }
    val matchedSnapTargets = remember { mutableStateMapOf<Int, Offset>() }
    val wordOrigins = remember { mutableStateMapOf<Int, Offset>() }

    // Idle 5s → hand guide demos dragging a word onto its matching picture. Only BEFORE the first
    // drop (teach the drag once); after the kid starts matching, no more hand.
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

    // Auto-advance to the next game once the final word's audio has finished
    // (isComplete is only set after playWordAndAwait completes in the ViewModel).
    LaunchedEffect(ui.isComplete) {
        if (ui.isComplete) {
            delay(1_000L)
            onGameComplete()
        }
    }

    Box(modifier = modifier.onGloballyPositioned { boxOrigin = it.boundsInWindow().topLeft }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val rows = ui.items.chunked(2)
            rows.forEachIndexed { rowIdx, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                ) {
                    rowItems.forEach { item ->
                        PictureSlot(
                            emoji = item.emoji,
                            filledWord = if (item.id in ui.matchedWordIndices) item.word else null,
                            shakeKey = ui.wrongAttemptKey,
                            onCenterPositioned = { center -> pictureCenters[item.id] = center },
                            modifier = Modifier.width(120.dp),
                        )
                    }
                }
                if (rowIdx < rows.lastIndex) Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.weight(1f, fill = true))

            // 2×2 grid of word chips — split 4 items into 2 rows of 2 so each chip is wider
            // and long words (e.g. "computer", "alligator") fit on a single line.
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ui.items.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        rowItems.forEach { item ->
                            val isUsed = item.id in ui.matchedWordIndices
                            DraggableWord(
                                word = item.word,
                                tint = item.tint,
                                isUsed = isUsed,
                                snapTarget = matchedSnapTargets[item.id],
                                onCenterPositioned = { wordOrigins[item.id] = it },
                                onDragEnd = { center ->
                                    interactionTick++
                                    val nearest = nearestPicture(center, pictureCenters, snapRadiusPx)
                                    val matched = onDrop(item.id, nearest)
                                    if (matched && nearest != null) {
                                        pictureCenters[nearest]?.let { slot ->
                                            matchedSnapTargets[item.id] = slot
                                        }
                                    }
                                    matched
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Pad odd-length last row (e.g. 3 items) so remaining cells balance.
                        repeat(2 - rowItems.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Hand guide: drag the first unmatched word onto its matching picture (same id).
        val firstUnmatched = ui.items.firstOrNull { it.id !in ui.matchedWordIndices }
        val from = firstUnmatched?.let { wordOrigins[it.id] }
        val to = firstUnmatched?.let { pictureCenters[it.id] }
        val steps = if (from != null && to != null) {
            persistentListOf(HandStep.Drag(from - boxOrigin, to - boxOrigin))
        } else {
            persistentListOf()
        }
        GameHandGuide(isVisible = showHint && steps.isNotEmpty(), steps = steps)
    }
}

private fun nearestPicture(
    tileCenter: Offset,
    pictureCenters: Map<Int, Offset>,
    radiusPx: Float,
): Int? {
    return pictureCenters
        .map { (id, center) -> id to hypot(center.x - tileCenter.x, center.y - tileCenter.y) }
        .filter { (_, dist) -> dist <= radiusPx }
        .minByOrNull { it.second }
        ?.first
}

private const val SNAP_RADIUS_DP = 110
