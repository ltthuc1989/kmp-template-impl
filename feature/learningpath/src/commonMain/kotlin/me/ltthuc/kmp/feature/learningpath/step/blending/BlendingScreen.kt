package me.ltthuc.kmp.feature.learningpath.step.blending

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.blending_title
import me.ltthuc.kmp.core.resource.blending_word_progress
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.chant_previous
import me.ltthuc.kmp.core.resource.identify_all_done_subtitle
import me.ltthuc.kmp.core.resource.identify_all_done_title
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

private const val STEP_INDEX = 4

@Composable
internal fun BlendingScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: BlendingViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
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
        BlendingContent(
            currentLesson = currentLesson,
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            onPlayBlendedWord = { word -> viewModel.playBlendedWord(currentLesson, word) },
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

private enum class BlendState { Initial, Blending, Complete }

@Composable
private fun BlendingContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    onPlayBlendedWord: (word: String) -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val vocab = remember(currentLesson.id) {
        currentLesson.words
    }
    val totalRounds = vocab.size

    var roundIndex by remember(currentLesson.id) { mutableStateOf(0) }
    var blendState by remember(currentLesson.id, roundIndex) { mutableStateOf(BlendState.Initial) }
    var activeLetterIndex by remember(currentLesson.id, roundIndex) { mutableStateOf(-1) }
    var mascotBouncing by remember(currentLesson.id, roundIndex) { mutableStateOf(false) }
    var finalOverlay by remember(currentLesson.id) { mutableStateOf<ScoreFeedback?>(null) }

    val currentVocab = vocab[roundIndex.coerceIn(0, vocab.lastIndex)]
    val letters = remember(currentVocab.text) {
        currentVocab.text.uppercase().toList().toImmutableList()
    }

    val letterDurationMs = remember(roundIndex) {
        (INITIAL_LETTER_MS - (roundIndex * SPEED_STEP_MS)).coerceAtLeast(MIN_LETTER_MS)
    }

    val scope = rememberCoroutineScope()
    val allDoneTitle = stringResource(Res.string.identify_all_done_title)
    val allDoneSubtitle = stringResource(Res.string.identify_all_done_subtitle, totalRounds)
    val successPrimary = stringResource(Res.string.score_success_primary)

    fun startBlend() {
        if (blendState == BlendState.Blending) return
        scope.launch {
            blendState = BlendState.Blending
            letters.indices.forEach { index ->
                activeLetterIndex = index
                delay(letterDurationMs.toLong())
            }
            activeLetterIndex = letters.lastIndex
            delay(WORD_PAUSE_MS)
            activeLetterIndex = -1
            onPlayBlendedWord(currentVocab.text)
            blendState = BlendState.Complete
            mascotBouncing = true
            delay(MASCOT_BOUNCE_MS)
            mascotBouncing = false
        }
    }

    fun onNextHandler() {
        if (roundIndex < totalRounds - 1) {
            roundIndex++
        } else {
            finalOverlay = ScoreFeedback.Success(
                title = allDoneTitle,
                subtitle = allDoneSubtitle,
                heroEmoji = vocab.firstOrNull()?.emoji.orEmpty().ifEmpty { "🎉" },
                primaryLabel = successPrimary,
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    title = stringResource(Res.string.blending_title),
                    currentStepIndex = STEP_INDEX,
                    onClose = onClose,
                    onStepJump = onStepJump,
                    stepSegments = stepSegments,
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
                RoundProgressIndicator(
                    currentRound = roundIndex,
                    totalRounds = totalRounds,
                )
                Spacer(Modifier.height(16.dp))
                MascotEmoji(
                    emoji = currentVocab.emoji.orEmpty().ifEmpty { "🎈" },
                    bouncing = mascotBouncing,
                )
                Spacer(Modifier.height(28.dp))
                LetterSweepRow(
                    letters = letters,
                    activeLetterIndex = activeLetterIndex,
                    isBlending = blendState == BlendState.Blending,
                )
                Spacer(Modifier.height(20.dp))
                CirclePlayButton(
                    enabled = blendState != BlendState.Blending,
                    onClick = ::startBlend,
                )
                Spacer(Modifier.weight(1f, fill = true))
                StepNavRow(
                    previousLabel = stringResource(Res.string.chant_previous),
                    nextLabel = stringResource(Res.string.chant_next),
                    onPrevious = onPrevious,
                    onNext = ::onNextHandler,
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
private fun RoundProgressIndicator(
    currentRound: Int,
    totalRounds: Int,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(totalRounds) { index ->
                val completed = index < currentRound
                val current = index == currentRound
                Icon(
                    imageVector = if (completed) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = when {
                        completed -> MaterialTheme.colorScheme.primary
                        current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.primaryContainer
                    },
                    modifier = Modifier.size(if (current) 24.dp else 20.dp),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.blending_word_progress, currentRound + 1, totalRounds),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MascotEmoji(emoji: String, bouncing: Boolean) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (bouncing) 1.18f else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "mascot-scale",
    )
    Text(
        text = emoji,
        fontSize = 72.sp,
        modifier = Modifier.scale(scaleAnim),
    )
}

@Composable
private fun LetterSweepRow(
    letters: ImmutableList<Char>,
    activeLetterIndex: Int,
    isBlending: Boolean,
) {
    if (letters.isEmpty()) return
    val letterCount = letters.size

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxRowWidth = maxWidth
        val gap = 8.dp
        val totalGap = gap * (letterCount - 1)
        val idealCardSize = (maxRowWidth - totalGap) / letterCount
        val cardSize = idealCardSize.coerceAtMost(MAX_CARD_SIZE)
        // Effective width of the actual letter row (cards + gaps). Used to center
        // arrow + trail + cards together when letters don't fill the full width.
        val effectiveRowWidth = cardSize * letterCount + gap * (letterCount - 1)

        val targetFraction = when {
            !isBlending && activeLetterIndex < 0 -> 0f
            activeLetterIndex < 0 -> 0f
            else -> (activeLetterIndex + 0.5f) / letterCount
        }
        val animatedFraction by animateFloatAsState(
            targetValue = targetFraction,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "sweep-fraction",
        )

        val trailFraction = when {
            activeLetterIndex < 0 -> 0f
            else -> ((activeLetterIndex + 1f) / letterCount).coerceIn(0f, 1f)
        }
        val animatedTrailFraction by animateFloatAsState(
            targetValue = trailFraction,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            label = "trail-fraction",
        )

        Column(
            modifier = Modifier
                .width(effectiveRowWidth)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DownArrowPointer(
                visible = isBlending || activeLetterIndex >= 0,
                fraction = animatedFraction,
                rowWidth = effectiveRowWidth,
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                letters.forEachIndexed { index, char ->
                    LetterCard(
                        char = char,
                        isActive = index == activeLetterIndex,
                        modifier = Modifier.size(cardSize),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrail(fraction = animatedTrailFraction)
        }
    }
}

@Composable
private fun DownArrowPointer(
    visible: Boolean,
    fraction: Float,
    rowWidth: Dp,
) {
    if (!visible) {
        Spacer(Modifier.height(ARROW_SIZE))
        return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ARROW_SIZE),
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .offset {
                    val widthPx = rowWidth.toPx()
                    val iconPx = ARROW_SIZE.toPx()
                    IntOffset(x = (fraction * widthPx - iconPx / 2f).toInt(), y = 0)
                }
                .size(ARROW_SIZE),
        )
    }
}

@Composable
private fun ProgressTrail(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(TRAIL_HEIGHT)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun LetterCard(
    char: Char,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.12f else 1f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "letter-scale",
    )
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val textColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.primary
    }
    PuffySurface(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale),
        shape = RoundedCornerShape(18.dp),
        containerColor = bgColor,
        shadowElevation = if (isActive) 16.dp else 8.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = if (isActive) 0.55f else 0.18f,
        topHighlightHeight = 6.dp,
        topHighlightAlpha = if (isActive) 0.3f else 0.85f,
        bottomShadeHeight = 6.dp,
        bottomShadeAlpha = if (isActive) 0.20f else 0.06f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = char.toString(),
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
            )
        }
    }
}

@Composable
private fun CirclePlayButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .size(72.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary, bounded = false),
                enabled = enabled,
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        },
        shadowElevation = 14.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = if (enabled) 0.55f else 0.30f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.3f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.25f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

// -------- Constants --------

private const val INITIAL_LETTER_MS = 400
private const val SPEED_STEP_MS = 80
private const val MIN_LETTER_MS = 180
private const val WORD_PAUSE_MS = 400L
private const val MASCOT_BOUNCE_MS = 500L
private val ARROW_SIZE = 28.dp
private val TRAIL_HEIGHT = 3.dp
private val MAX_CARD_SIZE = 84.dp
