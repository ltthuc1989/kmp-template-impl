package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.playAndAwait
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.step_guide_tracing
import me.ltthuc.kmp.core.resource.tracing_draw_here
import me.ltthuc.kmp.core.ui.audio.ScreenVoicePrompt
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.feature.learningpath.step.common.ConfettiCanvas
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

private const val STEP_INDEX = 6

@Composable
internal fun TracingScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: TracingViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    ScreenVoicePrompt("vp_step_trace")

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        LaunchedEffect(uiState.lessons.size) { onLessonsLoaded(uiState.lessons.size) }
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        TracingContent(
            currentLesson = uiState.lessons[safeIndex],
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

@Composable
private fun TracingContent(
    currentLesson: PhonicsLesson,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val letterChar = currentLesson.displayLetter.firstOrNull() ?: 'A'
    var isUppercase by remember(currentLesson.id) { mutableStateOf(true) }
    val guide = remember(letterChar, isUppercase) { LetterPaths.get(letterChar, isUppercase) }
    var celebrating by remember(currentLesson.id) { mutableStateOf(false) }
    // Count failed attempts for this lesson — after MAX_FAIL_ATTEMPTS, the fail overlay's button
    // lets the kid move on instead of retrying forever.
    var failCount by remember(currentLesson.id) { mutableStateOf(0) }
    val sfx = koinInject<SfxController>()
    val audioRepository = koinInject<AudioRepository>()
    val lang = LocalAppLanguage.current
    val tracingScope = rememberCoroutineScope()
    var navigated by remember(currentLesson.id) { mutableStateOf(false) }

    // State owned here so Next can read current strokes for scoring.
    val userStrokes = remember(currentLesson.id, isUppercase) {
        mutableStateListOf<SnapshotStateList<Offset>>()
    }
    var practiceCanvasSize by remember { mutableStateOf(Size.Zero) }
    val resetKey = remember(currentLesson.id, isUppercase) { mutableStateOf(0) }

    fun resetCanvas() {
        userStrokes.clear()
        resetKey.value += 1
    }

    fun submitForScoring() {
        val size = practiceCanvasSize
        if (size == Size.Zero) return
        val raw = TracingScorer.score(
            userStrokes = userStrokes.map { it.toList() },
            guide = guide,
            canvasSize = size,
        )
        val passed = raw >= TracingScorer.PASS_THRESHOLD
        if (passed) {
            // Vẽ đúng: KHÔNG popup — chỉ confetti + audio chúc mừng → tự về Lesson Map.
            celebrating = true
            sfx.playSfx("correct")
            tracingScope.launch {
                delay(TRACING_PRAISE_DELAY_MS)
                audioRepository.playAndAwait(AudioRef.Prompt("vp_lesson_done", lang), CONGRATS_MAX_MS)
                if (!navigated) {
                    navigated = true
                    onNext()
                }
            }
        } else {
            failCount += 1
            // Vẽ sai: không popup, chỉ nhắc audio "thử lại" rồi cho vẽ lại.
            sfx.playPrompt("vp_wrong", lang)
            resetCanvas()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    currentStepIndex = STEP_INDEX,
                    onClose = onClose,
                    onStepJump = onStepJump,
                    stepSegments = stepSegments,
                    guideText = stringResource(Res.string.step_guide_tracing),
                    showGuideText = false,
                )
            },
            bottomBar = {
                StepContinueButton(
                    label = stringResource(Res.string.common_next),
                    // After MAX_FAIL_ATTEMPTS, Next skips scoring + the fail popup and goes
                    // straight to the next step.
                    onClick = {
                        if (failCount >= MAX_FAIL_ATTEMPTS) onNext() else submitForScoring()
                    },
                    enabled = (failCount >= MAX_FAIL_ATTEMPTS) ||
                        (userStrokes.isNotEmpty() && !celebrating),
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
                Spacer(Modifier.height(6.dp))
                CaseToggle(
                    letterChar = letterChar,
                    uppercase = isUppercase,
                    onChange = {
                        isUppercase = it
                        resetCanvas()
                    },
                )
                Spacer(Modifier.height(10.dp))
                TracingGuideCard(guide = guide)
                Spacer(Modifier.height(10.dp))
                PracticeCanvas(
                    guide = guide,
                    userStrokes = userStrokes,
                    resetSignal = resetKey.value,
                    onCanvasSizeChanged = { practiceCanvasSize = it },
                    onReset = { resetCanvas() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // Confetti effect (KHÔNG popup card) trong lúc phát audio chúc mừng trước khi tự chuyển màn.
        if (celebrating) {
            ConfettiCanvas(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun CaseToggle(
    letterChar: Char,
    uppercase: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CasePill(
            label = letterChar.uppercaseChar().toString(),
            selected = uppercase,
            onClick = { onChange(true) },
        )
        CasePill(
            label = letterChar.lowercaseChar().toString(),
            selected = !uppercase,
            onClick = { onChange(false) },
        )
    }
}

@Composable
private fun CasePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

@Composable
private fun TracingGuideCard(guide: LetterGuide) {
    val primary = MaterialTheme.colorScheme.primary
    val halo = MaterialTheme.colorScheme.primaryContainer
    val playAnim = remember(guide.char, guide.uppercase) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }

    // Auto-play stroke animation on first mount + when letter or case changes
    LaunchedEffect(guide.char, guide.uppercase) {
        playAnim.snapTo(0f)
        playAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = guide.strokes.size * GUIDE_MS_PER_STROKE,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(GUIDE_CARD_HEIGHT_DP.dp),
        shape = RoundedCornerShape(GUIDE_CARD_CORNER_DP.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 10.dp,
        shadowTint = primary,
        shadowAlpha = 0.25f,
        topHighlightHeight = 14.dp,
        topHighlightAlpha = 0.6f,
        bottomShadeHeight = 14.dp,
        bottomShadeAlpha = 0.10f,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(GUIDE_CARD_PADDING_DP.dp),
        ) {
            canvasSizePx = size
            drawLetterGuide(
                guide = guide,
                canvasSize = size,
                animationProgress = playAnim.value,
                primaryColor = primary,
                haloColor = halo,
                strokeWidthPx = GUIDE_CARD_STROKE_WIDTH_PX,
                dashStrokeWidthPx = GUIDE_CARD_DASH_WIDTH_PX,
                showArrows = true,
            )
        }
        if (canvasSizePx != Size.Zero) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(GUIDE_CARD_PADDING_DP.dp),
            ) {
                StrokeNumberBadges(
                    guide = guide,
                    canvasSizePx = canvasSizePx,
                    badgeDiameter = 20,
                )
            }
        }
        IconButton(
            onClick = {
                scope.launch {
                    playAnim.snapTo(0f)
                    playAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = guide.strokes.size * GUIDE_MS_PER_STROKE,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Suppress("LongParameterList", "UnstableCollections", "MutableParams")
@Composable
private fun PracticeCanvas(
    guide: LetterGuide,
    userStrokes: SnapshotStateList<SnapshotStateList<Offset>>,
    resetSignal: Int,
    onCanvasSizeChanged: (Size) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val ghostColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)

    // Idle 5s with an empty canvas → a 👆 traces the letter to show the kid what to do. Any stroke
    // (userStrokes grows) or a reset re-arms the timer; drawing hides the hand immediately.
    var canvasSizePx by remember { mutableStateOf(Size.Zero) }
    var showHand by remember { mutableStateOf(false) }
    LaunchedEffect(resetSignal, userStrokes.size) {
        showHand = false
        if (userStrokes.isEmpty()) {
            delay(GUIDE_IDLE_MS)
            showHand = true
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PRACTICE_CORNER_DP.dp))
            .background(Color.White)
            .onSizeChanged { canvasSizePx = Size(it.width.toFloat(), it.height.toFloat()) },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(guide.char, guide.uppercase, resetSignal) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newStroke = mutableStateListOf(offset)
                            userStrokes.add(newStroke)
                        },
                        onDrag = { change, _ ->
                            userStrokes.lastOrNull()?.add(change.position)
                        },
                        onDragEnd = {},
                        onDragCancel = {},
                    )
                },
        ) {
            onCanvasSizeChanged(size)

            // Layer 1: Grid background (deepest)
            val cellPx = PRACTICE_GRID_CELL_PX
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += cellPx
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += cellPx
            }

            // Layer 2: Ghost letter — rendered from the same Zaner-Bloser SVG paths as the
            // guide card so the ghost shape matches the guide 1:1 (not a system-font glyph).
            val ghostStrokeWidthPx = minOf(size.width, size.height) * GHOST_STROKE_FRACTION
            drawGhostLetter(
                guide = guide,
                canvasSize = size,
                color = ghostColor,
                strokeWidthPx = ghostStrokeWidthPx,
            )

            // Layer 3: User strokes — one combined path so overlapping strokes merge cleanly.
            val combinedPath = Path()
            userStrokes.forEach { stroke ->
                if (stroke.size >= 2) {
                    combinedPath.moveTo(stroke[0].x, stroke[0].y)
                    for (i in 1 until stroke.size) {
                        combinedPath.lineTo(stroke[i].x, stroke[i].y)
                    }
                }
            }
            if (!combinedPath.isEmpty) {
                drawPath(
                    path = combinedPath,
                    color = primary,
                    style = Stroke(
                        width = PRACTICE_INK_WIDTH_PX,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        // "Draw here" hint (bottom-left)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Draw,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = stringResource(Res.string.tracing_draw_here).uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                letterSpacing = 1.5.sp,
            )
        }

        // Reset button (bottom-right)
        IconButton(
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showHand && canvasSizePx != Size.Zero) {
            TracingIdleHand(guide = guide, canvasSize = canvasSizePx)
        }
    }
}

/**
 * A 👆 that loops along the letter's strokes (same scaled paths as the rendered guide), shown on the
 * practice canvas after the kid has been idle. Positioned in the canvas's pixel coordinate space.
 */
@Composable
private fun TracingIdleHand(guide: LetterGuide, canvasSize: Size) {
    val measures = remember(guide, canvasSize) {
        scaledGuidePaths(guide, canvasSize).map { PathMeasure().apply { setPath(it, false) } }
    }
    val lengths = remember(measures) { measures.map { it.length } }
    val total = lengths.sum()
    if (total <= 0f) return

    val progress = remember(guide, canvasSize) { Animatable(0f) }
    // Trace the letter exactly once, then disappear — no looping.
    var finished by remember(guide, canvasSize) { mutableStateOf(false) }
    LaunchedEffect(guide, canvasSize) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = (measures.size * TRACE_HAND_MS_PER_STROKE)
                    .coerceAtLeast(TRACE_HAND_MS_PER_STROKE),
                easing = LinearEasing,
            ),
        )
        finished = true
    }
    if (finished) return

    // Map the global 0..1 progress onto the concatenated strokes and read the point there.
    var remaining = progress.value * total
    var point = Offset.Zero
    for (i in measures.indices) {
        if (remaining <= lengths[i] || i == measures.lastIndex) {
            val p = measures[i].getPosition(remaining.coerceIn(0f, lengths[i]))
            if (p.isSpecified) point = p
            break
        }
        remaining -= lengths[i]
    }

    Text(
        text = "👆",
        fontSize = TRACE_HAND_FONT_SP.sp,
        modifier = Modifier.offset {
            // Nudge so the fingertip (top-center of the emoji) lands on the path point.
            IntOffset(
                x = (point.x - TRACE_HAND_FONT_SP.dp.toPx() / 2f).roundToInt(),
                y = point.y.roundToInt(),
            )
        },
    )
}

private const val EXCELLENT_THRESHOLD = 95
private const val VERY_GOOD_THRESHOLD = 85
private const val MAX_FAIL_ATTEMPTS = 3
private const val TRACING_PRAISE_DELAY_MS = 500L
private const val CONGRATS_MAX_MS = 6_000L
private const val GUIDE_CARD_HEIGHT_DP = 210
private const val GUIDE_CARD_CORNER_DP = 24
private const val GUIDE_CARD_PADDING_DP = 20
private const val GUIDE_CARD_STROKE_WIDTH_PX = 22f
private const val GUIDE_CARD_DASH_WIDTH_PX = 3f
private const val GUIDE_MS_PER_STROKE = 1200
private const val PRACTICE_CORNER_DP = 24
private const val GHOST_STROKE_FRACTION = 0.10f
private const val PRACTICE_GRID_CELL_PX = 60f

// Idle delay before the tracing guide hand appears — same 5s beat as the mini-games.
private const val GUIDE_IDLE_MS = 5_000L
private const val TRACE_HAND_MS_PER_STROKE = 1100
private const val TRACE_HAND_FONT_SP = 40
private const val PRACTICE_INK_WIDTH_PX = 18f
