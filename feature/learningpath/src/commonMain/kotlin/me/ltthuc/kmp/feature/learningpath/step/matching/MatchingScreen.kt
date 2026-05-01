package me.ltthuc.kmp.feature.learningpath.step.matching

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
import androidx.compose.runtime.DisposableEffect
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
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.chant_previous
import me.ltthuc.kmp.core.resource.identify_all_done_subtitle
import me.ltthuc.kmp.core.resource.identify_all_done_title
import me.ltthuc.kmp.core.resource.matching_check_button
import me.ltthuc.kmp.core.resource.matching_hint
import me.ltthuc.kmp.core.resource.matching_title
import me.ltthuc.kmp.core.resource.score_success_primary
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StepNavRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private const val STEP_INDEX = 5

@Composable
internal fun MatchingScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
    totalSteps: Int = 7,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: MatchingViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        LaunchedEffect(uiState.lessons.size) { onLessonsLoaded(uiState.lessons.size) }
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        val currentLesson = uiState.lessons[safeIndex]
        MatchingContent(
            currentLesson = currentLesson,
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            onPlayWord = { word -> viewModel.onListenWord(currentLesson, word) },
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalSteps,
        )
    }
}

@Composable
private fun MatchingContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    onPlayWord: (word: String) -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    totalSteps: Int,
) {
    val vocab = remember(currentLesson.id) {
        currentLesson.words.toImmutableList()
    }
    // Auto-play first vocab word on enter to teach kid the "tap to listen" affordance.
    LaunchedEffect(currentLesson.id) {
        kotlinx.coroutines.delay(AUTO_PLAY_DELAY_MS)
        vocab.firstOrNull()?.let { onPlayWord(it.text) }
    }
    val rightOrder = remember(currentLesson.id) {
        vocab.shuffled(Random(currentLesson.id.hashCode())).toImmutableList()
    }
    val totalPairs = vocab.size

    // Dot positions captured from child composables (local to matching Box).
    val leftDotPositions = remember(currentLesson.id) { mutableStateMapOf<String, Offset>() }
    val rightDotPositions = remember(currentLesson.id) { mutableStateMapOf<String, Offset>() }
    // Draft pairings (may be wrong; not locked).
    val pendingMatches = remember(currentLesson.id) { mutableStateMapOf<String, String>() }
    // Validated correct pairings (locked, can't redo).
    val validatedMatches = remember(currentLesson.id) { mutableStateMapOf<String, String>() }
    var wrongFlashTexts by remember(currentLesson.id) { mutableStateOf(emptySet<String>()) }
    var finalOverlay by remember(currentLesson.id) { mutableStateOf<ScoreFeedback?>(null) }

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
                    currentStepIndex = STEP_INDEX,
                    onClose = onClose,
                    onStepJump = onStepJump,
                    totalSteps = totalSteps,
                )
            },
            bottomBar = {
                LetterStepperBar(
                    lessons = lessons,
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
                    wordKey = currentLesson.id,
                    leftItems = vocab,
                    rightItems = rightOrder,
                    pendingMatches = pendingMatches,
                    validatedMatches = validatedMatches,
                    wrongFlashTexts = wrongFlashTexts,
                    leftDotPositions = leftDotPositions,
                    rightDotPositions = rightDotPositions,
                    onPendingMatch = { leftText, rightText ->
                        pendingMatches[leftText] = rightText
                    },
                    onPlayWord = onPlayWord,
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

@Suppress("LongParameterList", "MutableParams", "UnstableCollections")
@Composable
private fun MatchingArea(
    wordKey: String,
    leftItems: ImmutableList<LessonWord>,
    rightItems: ImmutableList<LessonWord>,
    pendingMatches: Map<String, String>,
    validatedMatches: Map<String, String>,
    wrongFlashTexts: Set<String>,
    leftDotPositions: MutableMap<String, Offset>,
    rightDotPositions: MutableMap<String, Offset>,
    onPendingMatch: (String, String) -> Unit,
    onPlayWord: (String) -> Unit,
    isLocked: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
    var boxWindowOrigin by remember { mutableStateOf(Offset.Zero) }
    // Local drag state — must stay inside composable so pointerInput closures read fresh value.
    var dragLine by remember(wordKey) { mutableStateOf<DragLine?>(null) }

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
                        dragLine = DragLine(
                            fromLeftText = hit.key,
                            start = hit.value,
                            end = offset,
                        )
                    },
                    onDrag = { change, _ ->
                        val current = dragLine ?: return@detectDragGestures
                        dragLine = current.copy(end = change.position)
                    },
                    onDragEnd = {
                        val line = dragLine ?: return@detectDragGestures
                        val hitRight = rightDotPositions.entries.firstOrNull {
                            (it.value - line.end).distance() <= DOT_HIT_RADIUS_PX
                        }
                        if (hitRight != null) {
                            onPendingMatch(line.fromLeftText, hitRight.key)
                        }
                        dragLine = null
                    },
                    onDragCancel = { dragLine = null },
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
            onPlayWord = onPlayWord,
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

@Suppress("LongParameterList", "MutableParams", "UnstableCollections")
@Composable
private fun MatchingRows(
    leftItems: ImmutableList<LessonWord>,
    rightItems: ImmutableList<LessonWord>,
    pendingMatches: Map<String, String>,
    validatedMatches: Map<String, String>,
    wrongFlashTexts: Set<String>,
    boxWindowOrigin: Offset,
    leftDotPositions: MutableMap<String, Offset>,
    rightDotPositions: MutableMap<String, Offset>,
    onPlayWord: (String) -> Unit,
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
                            onClick = { onPlayWord(leftItem.text) },
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
    onClick: () -> Unit,
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
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            ),
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
    item: LessonWord,
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

// -------- Constants --------

private val DOT_SIZE = 14.dp
private val ART_SIZE = 56.dp
private const val DOT_HIT_RADIUS_PX = 70f
private const val WRONG_FLASH_MS = 700L
private const val AUTO_PLAY_DELAY_MS = 500L
