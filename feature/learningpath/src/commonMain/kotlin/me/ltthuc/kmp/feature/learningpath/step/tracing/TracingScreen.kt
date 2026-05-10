package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.chant_previous
import me.ltthuc.kmp.core.resource.score_fail_primary
import me.ltthuc.kmp.core.resource.score_fail_title
import me.ltthuc.kmp.core.resource.score_success_primary
import me.ltthuc.kmp.core.resource.score_success_title
import me.ltthuc.kmp.core.resource.step_guide_tracing
import me.ltthuc.kmp.core.resource.tracing_draw_here
import me.ltthuc.kmp.core.resource.tracing_fail_subtitle
import me.ltthuc.kmp.core.resource.tracing_score_excellent
import me.ltthuc.kmp.core.resource.tracing_score_good
import me.ltthuc.kmp.core.resource.tracing_score_very_good
import me.ltthuc.kmp.core.resource.tracing_title
import me.ltthuc.kmp.core.resource.tracing_watch_ad_to_pass
import me.ltthuc.kmp.core.ui.ads.RewardedSkipLauncher
import me.ltthuc.kmp.core.ui.ads.TracingInterstitial
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val STEP_INDEX = 6

@Composable
internal fun TracingScreen(
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
    viewModel: TracingViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    TracingInterstitial()
    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        LaunchedEffect(uiState.lessons.size) { onLessonsLoaded(uiState.lessons.size) }
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        TracingContent(
            currentLesson = uiState.lessons[safeIndex],
            lessons = uiState.lessons,
            currentIndex = safeIndex,
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
private fun TracingContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    onClose: () -> Unit,
    onHome: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val letterChar = currentLesson.displayLetter.firstOrNull() ?: 'A'
    var isUppercase by remember(currentLesson.id) { mutableStateOf(true) }
    val guide = remember(letterChar, isUppercase) { LetterPaths.get(letterChar, isUppercase) }
    var result by remember(currentLesson.id) { mutableStateOf<TracingResult?>(null) }

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
        val percent = (raw * 100).toInt().coerceIn(0, 100)
        result = TracingResult(percent = percent, passed = raw >= TracingScorer.PASS_THRESHOLD)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    title = stringResource(Res.string.tracing_title),
                    currentStepIndex = STEP_INDEX,
                    onClose = onClose,
                    onHomeClick = onHome,
                    onStepJump = onStepJump,
                    stepSegments = stepSegments,
                    guideText = stringResource(Res.string.step_guide_tracing),
                    showGuideText = false,
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
                Spacer(Modifier.height(10.dp))
                StepContinueButton(
                    label = stringResource(Res.string.chant_next),
                    onClick = { submitForScoring() },
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        val feedback = result?.let { r ->
            if (r.passed) {
                val tierLabel = when {
                    r.percent >= EXCELLENT_THRESHOLD -> stringResource(Res.string.tracing_score_excellent)
                    r.percent >= VERY_GOOD_THRESHOLD -> stringResource(Res.string.tracing_score_very_good)
                    else -> stringResource(Res.string.tracing_score_good)
                }
                ScoreFeedback.Success(
                    title = stringResource(Res.string.score_success_title),
                    subtitle = tierLabel,
                    heroEmoji = "🎉",
                    primaryLabel = stringResource(Res.string.score_success_primary),
                )
            } else {
                ScoreFeedback.Fail(
                    title = stringResource(Res.string.score_fail_title),
                    subtitle = stringResource(Res.string.tracing_fail_subtitle),
                    heroEmoji = "😔",
                    primaryLabel = stringResource(Res.string.score_fail_primary),
                    secondaryLabel = stringResource(Res.string.tracing_watch_ad_to_pass),
                )
            }
        }
        var showRewardedAd by remember { mutableStateOf(false) }
        ScoreFeedbackOverlay(
            feedback = feedback,
            onDismiss = { result = null },
            onPrimary = {
                val wasSuccess = result?.passed == true
                result = null
                if (wasSuccess) {
                    onNext()
                } else {
                    resetCanvas()
                }
            },
            onSecondary = { showRewardedAd = true },
        )
        if (showRewardedAd) {
            RewardedSkipLauncher(
                onRewardEarned = {
                    showRewardedAd = false
                    result = null
                    onNext()
                },
                onUnavailable = {
                    showRewardedAd = false
                },
            )
        }
    }
}

@androidx.compose.runtime.Immutable
private data class TracingResult(val percent: Int, val passed: Boolean)

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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PRACTICE_CORNER_DP.dp))
            .background(Color.White),
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
    }
}

private const val EXCELLENT_THRESHOLD = 95
private const val VERY_GOOD_THRESHOLD = 85
private const val GUIDE_CARD_HEIGHT_DP = 210
private const val GUIDE_CARD_CORNER_DP = 24
private const val GUIDE_CARD_PADDING_DP = 20
private const val GUIDE_CARD_STROKE_WIDTH_PX = 22f
private const val GUIDE_CARD_DASH_WIDTH_PX = 3f
private const val GUIDE_MS_PER_STROKE = 1200
private const val PRACTICE_CORNER_DP = 24
private const val GHOST_STROKE_FRACTION = 0.14f
private const val PRACTICE_GRID_CELL_PX = 60f
private const val PRACTICE_INK_WIDTH_PX = 18f
