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
import androidx.compose.ui.geometry.Rect
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
import me.ltthuc.kmp.core.resource.identify_all_done_subtitle
import me.ltthuc.kmp.core.resource.identify_all_done_title
import me.ltthuc.kmp.core.resource.matching_title
import me.ltthuc.kmp.core.resource.score_success_primary
import me.ltthuc.kmp.core.resource.step_guide_matching
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
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
    onHome: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
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
            onPlaySfx = viewModel::playSfx,
            onPlayVoice = viewModel::playVoicePraise,
            onClose = onClose,
            onHome = onHome,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

@Composable
private fun MatchingContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    onPlayWord: (word: String) -> Unit,
    onPlaySfx: (String) -> Unit,
    onPlayVoice: (String) -> Unit,
    onClose: () -> Unit,
    onHome: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
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
    // Full card bounds (box-local coords) — drag-anywhere hit area, not just the small dot.
    val leftCardBounds = remember(currentLesson.id) { mutableStateMapOf<String, Rect>() }
    val rightCardBounds = remember(currentLesson.id) { mutableStateMapOf<String, Rect>() }
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

    fun onMatchAttempt(leftText: String, rightText: String) {
        // Reject duplicate target — current pending entry pointing to this rightText must clear first
        pendingMatches.entries.removeAll { it.value == rightText }
        val isCorrect = leftText.equals(rightText, ignoreCase = true)
        if (isCorrect) {
            validatedMatches[leftText] = rightText
            // Khan-simple: chime + voice praise after delay on every correct match.
            onPlaySfx(SFX_CORRECT)
            scope.launch {
                delay(PRAISE_DELAY_MS)
                onPlayVoice(MATCHING_PRAISE_POOL.random())
            }
            if (validatedMatches.size == totalPairs) {
                onPlaySfx(SFX_LESSON_COMPLETE)
                finalOverlay = ScoreFeedback.Success(
                    title = allDoneTitle,
                    subtitle = allDoneSubtitle,
                    heroEmoji = heroEmoji,
                    primaryLabel = successPrimary,
                )
            }
        } else {
            // Khan-simple: NO SFX on wrong. Visual shake + dashed line is the feedback.
            pendingMatches[leftText] = rightText
            wrongFlashTexts = wrongFlashTexts + leftText
            scope.launch {
                delay(WRONG_FLASH_MS)
                pendingMatches.remove(leftText)
                wrongFlashTexts = wrongFlashTexts - leftText
            }
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
                    onHomeClick = onHome,
                    onStepJump = onStepJump,
                    stepSegments = stepSegments,
                    guideText = stringResource(Res.string.step_guide_matching),
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
                MatchingArea(
                    wordKey = currentLesson.id,
                    leftItems = vocab,
                    rightItems = rightOrder,
                    pendingMatches = pendingMatches,
                    validatedMatches = validatedMatches,
                    wrongFlashTexts = wrongFlashTexts,
                    leftDotPositions = leftDotPositions,
                    rightDotPositions = rightDotPositions,
                    leftCardBounds = leftCardBounds,
                    rightCardBounds = rightCardBounds,
                    onPendingMatch = ::onMatchAttempt,
                    onPlayWord = onPlayWord,
                    isLockedLeft = { leftText -> validatedMatches.containsKey(leftText) },
                    isLockedRight = { rightText -> validatedMatches.containsValue(rightText) },
                )
                Spacer(Modifier.weight(1f, fill = true))
                Spacer(Modifier.height(8.dp))
                StepContinueButton(
                    label = stringResource(Res.string.chant_next),
                    onClick = onNext,
                    enabled = validatedMatches.size == totalPairs && finalOverlay == null,
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
    leftCardBounds: MutableMap<String, Rect>,
    rightCardBounds: MutableMap<String, Rect>,
    onPendingMatch: (String, String) -> Unit,
    onPlayWord: (String) -> Unit,
    isLockedLeft: (String) -> Boolean,
    isLockedRight: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val errorColor = MaterialTheme.colorScheme.error
    val dashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f) }
    var boxWindowOrigin by remember { mutableStateOf(Offset.Zero) }
    // Local drag state — must stay inside composable so pointerInput closures read fresh value.
    var dragLine by remember(wordKey) { mutableStateOf<DragLine?>(null) }
    var hintDismissed by remember(wordKey) { mutableStateOf(false) }
    val hintVisible = !hintDismissed &&
        pendingMatches.isEmpty() &&
        validatedMatches.isEmpty() &&
        dragLine == null &&
        leftDotPositions.isNotEmpty() &&
        rightDotPositions.isNotEmpty() &&
        leftItems.isNotEmpty() &&
        rightItems.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                boxWindowOrigin = coords.positionInWindow()
            }
            .pointerInput(wordKey) {
                detectDragGestures(
                    onDragStart = { offset ->
                        hintDismissed = true
                        // Drag-anywhere: hit-test against full card bounds, not just the dot.
                        // Line still anchors at the dot position so the drawn connector looks right.
                        val leftHit = leftCardBounds.entries.firstOrNull { (_, rect) -> rect.contains(offset) }
                        if (leftHit != null) {
                            if (isLockedLeft(leftHit.key)) return@detectDragGestures
                            val anchor = leftDotPositions[leftHit.key] ?: return@detectDragGestures
                            dragLine = DragLine(
                                fromKey = leftHit.key,
                                fromSide = DragSide.LEFT,
                                start = anchor,
                                end = offset,
                            )
                            return@detectDragGestures
                        }
                        val rightHit = rightCardBounds.entries.firstOrNull { (_, rect) -> rect.contains(offset) }
                            ?: return@detectDragGestures
                        if (isLockedRight(rightHit.key)) return@detectDragGestures
                        val anchor = rightDotPositions[rightHit.key] ?: return@detectDragGestures
                        dragLine = DragLine(
                            fromKey = rightHit.key,
                            fromSide = DragSide.RIGHT,
                            start = anchor,
                            end = offset,
                        )
                    },
                    onDrag = { change, _ ->
                        val current = dragLine ?: return@detectDragGestures
                        dragLine = current.copy(end = change.position)
                    },
                    onDragEnd = {
                        val line = dragLine ?: return@detectDragGestures
                        when (line.fromSide) {
                            DragSide.LEFT -> {
                                val hitRight = rightCardBounds.entries.firstOrNull { (_, rect) ->
                                    rect.contains(line.end)
                                }
                                if (hitRight != null && !isLockedRight(hitRight.key)) {
                                    onPendingMatch(line.fromKey, hitRight.key)
                                }
                            }
                            DragSide.RIGHT -> {
                                val hitLeft = leftCardBounds.entries.firstOrNull { (_, rect) ->
                                    rect.contains(line.end)
                                }
                                if (hitLeft != null && !isLockedLeft(hitLeft.key)) {
                                    onPendingMatch(hitLeft.key, line.fromKey)
                                }
                            }
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
            leftCardBounds = leftCardBounds,
            rightCardBounds = rightCardBounds,
            onPlayWord = onPlayWord,
        )
        MatchingDragHint(
            isVisible = hintVisible,
            leftItems = leftItems,
            rightItems = rightItems,
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
    leftCardBounds: MutableMap<String, Rect>,
    rightCardBounds: MutableMap<String, Rect>,
    onPlayWord: (String) -> Unit,
) {
    val rowCount = maxOf(leftItems.size, rightItems.size)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
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
                            onCardPositioned = { absoluteRect ->
                                leftCardBounds[leftItem.text] = absoluteRect.translate(
                                    -boxWindowOrigin.x,
                                    -boxWindowOrigin.y,
                                )
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
                            onClick = { onPlayWord(rightItem.text) },
                            onDotPositioned = { absolute ->
                                rightDotPositions[rightItem.text] = absolute - boxWindowOrigin
                            },
                            onCardPositioned = { absoluteRect ->
                                rightCardBounds[rightItem.text] = absoluteRect.translate(
                                    -boxWindowOrigin.x,
                                    -boxWindowOrigin.y,
                                )
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
    onCardPositioned: (Rect) -> Unit,
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
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val s = coords.size
                onCardPositioned(
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + s.width,
                        bottom = pos.y + s.height,
                    ),
                )
            }
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
    onClick: () -> Unit,
    onDotPositioned: (Offset) -> Unit,
    onCardPositioned: (Rect) -> Unit,
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
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .width(120.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val s = coords.size
                onCardPositioned(
                    Rect(
                        left = pos.x,
                        top = pos.y,
                        right = pos.x + s.width,
                        bottom = pos.y + s.height,
                    ),
                )
            }
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
                WordDisplayView(
                    word = item,
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

// -------- Helpers --------

internal enum class DragSide { LEFT, RIGHT }

private data class DragLine(
    val fromKey: String,
    val fromSide: DragSide,
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
private const val SFX_CORRECT = "correct"
private const val SFX_LESSON_COMPLETE = "lesson_complete"
private const val PRAISE_DELAY_MS = 500L
private val MATCHING_PRAISE_POOL = listOf(
    "praise_great_job",
    "praise_nice",
    "praise_you_got_it",
    "praise_well_done",
)
