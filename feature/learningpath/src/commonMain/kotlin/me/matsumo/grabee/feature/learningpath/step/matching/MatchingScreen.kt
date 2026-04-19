package me.matsumo.grabee.feature.learningpath.step.matching

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.matsumo.grabee.core.model.VocabularyItem
import me.matsumo.grabee.core.model.Word
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.chant_next
import me.matsumo.grabee.core.resource.chant_previous
import me.matsumo.grabee.core.resource.identify_all_done_subtitle
import me.matsumo.grabee.core.resource.identify_all_done_title
import me.matsumo.grabee.core.resource.matching_check_button
import me.matsumo.grabee.core.resource.matching_hint
import me.matsumo.grabee.core.resource.matching_title
import me.matsumo.grabee.core.resource.score_success_primary
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PuffySurface
import me.matsumo.grabee.feature.learningpath.step.common.ScoreFeedback
import me.matsumo.grabee.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import me.matsumo.grabee.feature.learningpath.step.common.StepNavRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

@Composable
internal fun MatchingScreen(
    unitId: String,
    wordIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MatchingViewModel = koinViewModel { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val safeIndex = wordIndex.coerceIn(0, uiState.words.lastIndex)
        MatchingContent(
            currentWord = uiState.words[safeIndex],
            words = uiState.words,
            currentIndex = safeIndex,
            totalWords = uiState.words.size,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
        )
    }
}

@Composable
private fun MatchingContent(
    currentWord: Word,
    words: ImmutableList<Word>,
    currentIndex: Int,
    totalWords: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val vocab = remember(currentWord.id) {
        currentWord.vocabulary.ifEmpty { listOf(currentWord.toVocabItem()) }.toImmutableList()
    }
    val rightOrder = remember(currentWord.id) {
        vocab.shuffled(Random(currentWord.id.hashCode())).toImmutableList()
    }
    val totalPairs = vocab.size

    // Dot positions captured from child composables (local to matching Box).
    val leftDotPositions = remember(currentWord.id) { mutableStateMapOf<String, Offset>() }
    val rightDotPositions = remember(currentWord.id) { mutableStateMapOf<String, Offset>() }
    // Draft pairings (may be wrong; not locked).
    val pendingMatches = remember(currentWord.id) { mutableStateMapOf<String, String>() }
    // Validated correct pairings (locked, can't redo).
    val validatedMatches = remember(currentWord.id) { mutableStateMapOf<String, String>() }
    var wrongFlashTexts by remember(currentWord.id) { mutableStateOf(emptySet<String>()) }
    var dragLine by remember(currentWord.id) { mutableStateOf<DragLine?>(null) }
    var finalOverlay by remember(currentWord.id) { mutableStateOf<ScoreFeedback?>(null) }

    val allDoneTitle = stringResource(Res.string.identify_all_done_title)
    val allDoneSubtitle = stringResource(Res.string.identify_all_done_subtitle, totalPairs)
    val successPrimary = stringResource(Res.string.score_success_primary)
    val heroEmoji = vocab.firstOrNull()?.emoji.orEmpty().ifEmpty { "🎉" }

    val scope = rememberCoroutineScope()

    val canCheck = pendingMatches.isNotEmpty() &&
        (pendingMatches.size + validatedMatches.size) == totalPairs

    fun performCheck() {
        val wrongSet = mutableSetOf<String>()
        val correctEntries = mutableListOf<Pair<String, String>>()
        pendingMatches.forEach { (leftText, rightText) ->
            if (leftText.equals(rightText, ignoreCase = true)) {
                correctEntries += leftText to rightText
            } else {
                wrongSet += leftText
            }
        }
        correctEntries.forEach { (l, r) ->
            validatedMatches[l] = r
            pendingMatches.remove(l)
        }
        if (wrongSet.isNotEmpty()) {
            wrongFlashTexts = wrongSet.toSet()
            scope.launch {
                delay(WRONG_FLASH_MS)
                wrongSet.forEach { pendingMatches.remove(it) }
                wrongFlashTexts = emptySet()
            }
        }
        if (validatedMatches.size == totalPairs) {
            finalOverlay = ScoreFeedback.Success(
                title = allDoneTitle,
                subtitle = allDoneSubtitle,
                heroEmoji = heroEmoji,
                primaryLabel = successPrimary,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    title = stringResource(Res.string.matching_title),
                    currentIndex = currentIndex,
                    totalWords = totalWords,
                    onClose = onClose,
                )
            },
            bottomBar = {
                LetterStepperBar(
                    words = words,
                    currentIndex = currentIndex,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(8.dp))
                HintPill()
                Spacer(Modifier.height(16.dp))
                MatchingArea(
                    wordKey = currentWord.id,
                    leftItems = vocab,
                    rightItems = rightOrder,
                    pendingMatches = pendingMatches,
                    validatedMatches = validatedMatches,
                    wrongFlashTexts = wrongFlashTexts,
                    dragLine = dragLine,
                    leftDotPositions = leftDotPositions,
                    rightDotPositions = rightDotPositions,
                    onDragLineChange = { line -> dragLine = line },
                    onPendingMatch = { leftText, rightText ->
                        pendingMatches[leftText] = rightText
                    },
                    isLocked = { leftText -> validatedMatches.containsKey(leftText) },
                    modifier = Modifier.weight(1f, fill = true),
                )
                Spacer(Modifier.height(12.dp))
                CheckButton(enabled = canCheck, onClick = ::performCheck)
                Spacer(Modifier.height(8.dp))
                StepNavRow(
                    previousLabel = stringResource(Res.string.chant_previous),
                    nextLabel = stringResource(Res.string.chant_next),
                    onPrevious = onPrevious,
                    onNext = onNext,
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        ScoreFeedbackOverlay(
            feedback = finalOverlay,
            onDismiss = { finalOverlay = null },
            onPrimary = {
                finalOverlay = null
                onNext()
            },
        )
    }
}

@Composable
private fun HintPill() {
    PuffySurface(
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 6.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.15f,
        topHighlightHeight = 6.dp,
        topHighlightAlpha = 0.85f,
        bottomShadeHeight = 6.dp,
        bottomShadeAlpha = 0.06f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.matching_hint),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun MatchingArea(
    wordKey: String,
    leftItems: ImmutableList<VocabularyItem>,
    rightItems: ImmutableList<VocabularyItem>,
    pendingMatches: Map<String, String>,
    validatedMatches: Map<String, String>,
    wrongFlashTexts: Set<String>,
    dragLine: DragLine?,
    leftDotPositions: MutableMap<String, Offset>,
    rightDotPositions: MutableMap<String, Offset>,
    onDragLineChange: (DragLine?) -> Unit,
    onPendingMatch: (String, String) -> Unit,
    isLocked: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
    var boxWindowOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                boxWindowOrigin = coords.positionInWindow()
            }
            .pointerInput(wordKey) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val hit = leftDotPositions.entries.firstOrNull {
                            (it.value - offset).distance() <= DOT_HIT_RADIUS_PX
                        } ?: return@detectDragGestures
                        if (isLocked(hit.key)) return@detectDragGestures
                        onDragLineChange(
                            DragLine(
                                fromLeftText = hit.key,
                                start = hit.value,
                                end = offset,
                            ),
                        )
                    },
                    onDrag = { change, _ ->
                        val current = dragLine ?: return@detectDragGestures
                        onDragLineChange(current.copy(end = change.position))
                    },
                    onDragEnd = {
                        val line = dragLine ?: return@detectDragGestures
                        val hitRight = rightDotPositions.entries.firstOrNull {
                            (it.value - line.end).distance() <= DOT_HIT_RADIUS_PX
                        }
                        if (hitRight != null) {
                            onPendingMatch(line.fromLeftText, hitRight.key)
                        }
                        onDragLineChange(null)
                    },
                    onDragCancel = { onDragLineChange(null) },
                )
            },
    ) {
        MatchingRows(
            leftItems = leftItems,
            rightItems = rightItems,
            pendingMatches = pendingMatches,
            validatedMatches = validatedMatches,
            wrongFlashTexts = wrongFlashTexts,
            boxWindowOrigin = boxWindowOrigin,
            leftDotPositions = leftDotPositions,
            rightDotPositions = rightDotPositions,
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Validated correct — solid primary
            validatedMatches.forEach { (leftText, rightText) ->
                val from = leftDotPositions[leftText]
                val to = rightDotPositions[rightText]
                if (from != null && to != null) {
                    drawLine(
                        color = primary,
                        start = from,
                        end = to,
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            // Pending (draft) — dashed neutral. Wrong flash overrides color to error.
            pendingMatches.forEach { (leftText, rightText) ->
                val from = leftDotPositions[leftText]
                val to = rightDotPositions[rightText]
                if (from != null && to != null) {
                    val isWrong = wrongFlashTexts.contains(leftText)
                    drawLine(
                        color = if (isWrong) errorColor else primary.copy(alpha = 0.45f),
                        start = from,
                        end = to,
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = dashEffect,
                    )
                }
            }
            // Current drag — dashed primary
            dragLine?.let {
                drawLine(
                    color = primary.copy(alpha = 0.55f),
                    start = it.start,
                    end = it.end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect,
                )
            }
            // Suppress unused warning (primaryContainer reserved for future dot styling)
            primaryContainer.hashCode()
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun MatchingRows(
    leftItems: ImmutableList<VocabularyItem>,
    rightItems: ImmutableList<VocabularyItem>,
    pendingMatches: Map<String, String>,
    validatedMatches: Map<String, String>,
    wrongFlashTexts: Set<String>,
    boxWindowOrigin: Offset,
    leftDotPositions: MutableMap<String, Offset>,
    rightDotPositions: MutableMap<String, Offset>,
) {
    val rowCount = maxOf(leftItems.size, rightItems.size)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        for (rowIndex in 0 until rowCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val leftItem = leftItems.getOrNull(rowIndex)
                    if (leftItem != null) {
                        val validated = validatedMatches.containsKey(leftItem.text)
                        val pending = pendingMatches.containsKey(leftItem.text)
                        val wrongFlash = wrongFlashTexts.contains(leftItem.text)
                        WordPill(
                            text = leftItem.text,
                            state = pillState(validated, pending, wrongFlash),
                            wrongKey = if (wrongFlash) wrongFlashTexts.hashCode() else 0,
                            onDotPositioned = { absolute ->
                                leftDotPositions[leftItem.text] = absolute - boxWindowOrigin
                            },
                        )
                    }
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    val rightItem = rightItems.getOrNull(rowIndex)
                    if (rightItem != null) {
                        val validated = validatedMatches.containsValue(rightItem.text)
                        val pending = pendingMatches.containsValue(rightItem.text)
                        val wrongFlash = pendingMatches.entries.any {
                            it.value == rightItem.text && wrongFlashTexts.contains(it.key)
                        }
                        ImageCard(
                            item = rightItem,
                            state = pillState(validated, pending, wrongFlash),
                            wrongKey = if (wrongFlash) wrongFlashTexts.hashCode() else 0,
                            onDotPositioned = { absolute ->
                                rightDotPositions[rightItem.text] = absolute - boxWindowOrigin
                            },
                        )
                    }
                }
            }
        }
    }
}

private enum class SlotState { Idle, Pending, Validated, WrongFlash }

private fun pillState(validated: Boolean, pending: Boolean, wrongFlash: Boolean): SlotState = when {
    wrongFlash -> SlotState.WrongFlash
    validated -> SlotState.Validated
    pending -> SlotState.Pending
    else -> SlotState.Idle
}

@Composable
private fun WordPill(
    text: String,
    state: SlotState,
    wrongKey: Int,
    onDotPositioned: (Offset) -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(wrongKey) {
        if (wrongKey != 0) {
            rotation.snapTo(0f)
            rotation.animateTo(-8f, tween(80))
            rotation.animateTo(8f, tween(80))
            rotation.animateTo(-5f, tween(80))
            rotation.animateTo(0f, tween(80))
        }
    }
    val borderColor = when (state) {
        SlotState.Validated -> MaterialTheme.colorScheme.primary
        SlotState.WrongFlash -> MaterialTheme.colorScheme.error
        SlotState.Pending -> MaterialTheme.colorScheme.primaryContainer
        SlotState.Idle -> MaterialTheme.colorScheme.primaryContainer
    }
    val borderWidth = if (state == SlotState.Validated || state == SlotState.WrongFlash) 2.dp else 1.dp
    val shadowAlpha = when (state) {
        SlotState.Validated -> 0.35f
        SlotState.Pending, SlotState.WrongFlash -> 0.18f
        SlotState.Idle -> 0.12f
    }
    val elevation = when (state) {
        SlotState.Validated -> 10.dp
        SlotState.Pending, SlotState.WrongFlash -> 8.dp
        SlotState.Idle -> 6.dp
    }
    val shape = CircleShape
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, shape),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = elevation,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = shadowAlpha,
        topHighlightHeight = 6.dp,
        topHighlightAlpha = 0.85f,
        bottomShadeHeight = 6.dp,
        bottomShadeAlpha = 0.06f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnchorDot(
                state = state,
                modifier = Modifier.onGloballyPositioned { coords ->
                    val windowPos = coords.positionInWindow()
                    val s = coords.size
                    onDotPositioned(windowPos + Offset(s.width / 2f, s.height / 2f))
                },
            )
        }
    }
}

@Composable
private fun ImageCard(
    item: VocabularyItem,
    state: SlotState,
    wrongKey: Int,
    onDotPositioned: (Offset) -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(wrongKey) {
        if (wrongKey != 0) {
            rotation.snapTo(0f)
            rotation.animateTo(-8f, tween(80))
            rotation.animateTo(8f, tween(80))
            rotation.animateTo(-5f, tween(80))
            rotation.animateTo(0f, tween(80))
        }
    }
    val borderColor = when (state) {
        SlotState.Validated -> MaterialTheme.colorScheme.primary
        SlotState.WrongFlash -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val borderWidth = if (state == SlotState.Validated || state == SlotState.WrongFlash) 2.dp else 1.dp
    val shadowAlpha = when (state) {
        SlotState.Validated -> 0.35f
        SlotState.Pending, SlotState.WrongFlash -> 0.18f
        SlotState.Idle -> 0.12f
    }
    val elevation = when (state) {
        SlotState.Validated -> 10.dp
        SlotState.Pending, SlotState.WrongFlash -> 8.dp
        SlotState.Idle -> 6.dp
    }
    val shape = RoundedCornerShape(20.dp)
    PuffySurface(
        modifier = Modifier
            .width(120.dp)
            .border(borderWidth, borderColor, shape),
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = elevation,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = shadowAlpha,
        topHighlightHeight = 6.dp,
        topHighlightAlpha = 0.85f,
        bottomShadeHeight = 6.dp,
        bottomShadeAlpha = 0.06f,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnchorDot(
                state = state,
                modifier = Modifier.onGloballyPositioned { coords ->
                    val windowPos = coords.positionInWindow()
                    val s = coords.size
                    onDotPositioned(windowPos + Offset(s.width / 2f, s.height / 2f))
                },
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(ART_SIZE)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.emoji.orEmpty().ifEmpty { "🎨" },
                    fontSize = 32.sp,
                )
            }
        }
    }
}

@Composable
private fun AnchorDot(
    state: SlotState,
    modifier: Modifier = Modifier,
) {
    val dotColor = when (state) {
        SlotState.Validated -> MaterialTheme.colorScheme.primary
        SlotState.Pending -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        SlotState.WrongFlash -> MaterialTheme.colorScheme.error
        SlotState.Idle -> MaterialTheme.colorScheme.primaryContainer
    }
    Box(
        modifier = modifier
            .size(DOT_SIZE)
            .clip(CircleShape)
            .background(dotColor),
    )
}

@Composable
private fun CheckButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                enabled = enabled,
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = containerColor,
        shadowElevation = 14.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = if (enabled) 0.55f else 0.30f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.3f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.30f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.matching_check_button),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

// -------- Helpers --------

private data class DragLine(
    val fromLeftText: String,
    val start: Offset,
    val end: Offset,
)

private fun Offset.distance(): Float = kotlin.math.sqrt(x * x + y * y)

private fun Word.toVocabItem(): VocabularyItem = VocabularyItem(
    text = text.replaceFirstChar { it.uppercase() },
    emoji = emoji,
    imageAsset = imageAsset,
    orderIndex = 0,
)

// -------- Constants --------

private val DOT_SIZE = 14.dp
private val ART_SIZE = 56.dp
private const val DOT_HIT_RADIUS_PX = 70f
private const val WRONG_FLASH_MS = 700L
