package me.matsumo.grabee.feature.learningpath.step.identify

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.matsumo.grabee.core.model.LessonWord
import me.matsumo.grabee.core.model.PhonicsLesson
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.chant_next
import me.matsumo.grabee.core.resource.chant_previous
import me.matsumo.grabee.core.resource.identify_all_done_soft_subtitle
import me.matsumo.grabee.core.resource.identify_all_done_subtitle
import me.matsumo.grabee.core.resource.identify_all_done_title
import me.matsumo.grabee.core.resource.identify_instruction
import me.matsumo.grabee.core.resource.identify_listen_cd
import me.matsumo.grabee.core.resource.identify_round_progress
import me.matsumo.grabee.core.resource.identify_title
import me.matsumo.grabee.core.resource.score_success_primary
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PuffySurface
import me.matsumo.grabee.feature.learningpath.step.common.PulseRings
import me.matsumo.grabee.feature.learningpath.step.common.ScoreFeedback
import me.matsumo.grabee.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import me.matsumo.grabee.feature.learningpath.step.common.StepNavRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private const val STEP_INDEX = 3

@Composable
internal fun IdentifyScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IdentifyViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        IdentifyContent(
            currentLesson = uiState.lessons[safeIndex],
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
        )
    }
}

@Composable
private fun IdentifyContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
) {
    val vocab = remember(currentLesson.id) {
        currentLesson.words
    }
    val totalRounds = vocab.size
    val otherWords = remember(currentLesson.id, lessons) {
        lessons.filter { it.id != currentLesson.id }
    }

    var roundIndex by remember(currentLesson.id) { mutableStateOf(0) }
    var wrongCount by remember(currentLesson.id, roundIndex) { mutableStateOf(0) }
    var selectedText by remember(currentLesson.id, roundIndex) { mutableStateOf<String?>(null) }
    var wiggleTarget by remember(currentLesson.id, roundIndex) { mutableStateOf<String?>(null) }
    var wiggleKey by remember(currentLesson.id, roundIndex) { mutableStateOf(0) }
    var showHint by remember(currentLesson.id, roundIndex) { mutableStateOf(false) }
    var autoRevealed by remember(currentLesson.id, roundIndex) { mutableStateOf(false) }
    var listenPlaying by remember(currentLesson.id, roundIndex) { mutableStateOf(true) }
    var finalOverlay by remember(currentLesson.id) { mutableStateOf<ScoreFeedback?>(null) }
    var anyRoundFailed by remember(currentLesson.id) { mutableStateOf(false) }

    val target = vocab[roundIndex.coerceIn(0, vocab.lastIndex)]
    val gridItems = remember(currentLesson.id, roundIndex) {
        buildRoundGrid(
            target = target,
            sameLetterVocab = vocab,
            otherWords = otherWords,
            seed = "${currentLesson.id}-$roundIndex".hashCode(),
        )
    }

    val successPrimary = stringResource(Res.string.score_success_primary)
    val allDoneTitle = stringResource(Res.string.identify_all_done_title)
    val allDoneSubtitle = stringResource(Res.string.identify_all_done_subtitle, totalRounds)
    val allDoneSoftSubtitle = stringResource(Res.string.identify_all_done_soft_subtitle)

    val scope = rememberCoroutineScope()

    // Audio autoplay stub: hide grid, listenPlaying → true → delay 1s → reveal grid
    LaunchedEffect(currentLesson.id, roundIndex) {
        listenPlaying = true
        delay(LISTEN_DURATION_MS)
        listenPlaying = false
    }

    val onCardTap: (String) -> Unit = { tappedText ->
        if (selectedText == null && !autoRevealed && !listenPlaying) {
            if (tappedText == target.text) {
                selectedText = tappedText
                scope.launch {
                    delay(CORRECT_CELEBRATION_MS)
                    selectedText = null
                    if (roundIndex < totalRounds - 1) {
                        roundIndex++
                    } else {
                        finalOverlay = ScoreFeedback.Success(
                            title = allDoneTitle,
                            subtitle = if (anyRoundFailed) allDoneSoftSubtitle else allDoneSubtitle,
                            heroEmoji = vocab.firstOrNull()?.emoji.orEmpty().ifEmpty { "🎉" },
                            primaryLabel = successPrimary,
                        )
                    }
                }
            } else {
                wrongCount++
                wiggleTarget = tappedText
                wiggleKey++
                when {
                    wrongCount >= MAX_WRONG_BEFORE_REVEAL -> {
                        autoRevealed = true
                        anyRoundFailed = true
                        scope.launch {
                            delay(AUTO_REVEAL_DELAY_MS)
                            if (roundIndex < totalRounds - 1) {
                                roundIndex++
                            } else {
                                finalOverlay = ScoreFeedback.Success(
                                    title = allDoneTitle,
                                    subtitle = allDoneSoftSubtitle,
                                    heroEmoji = vocab.firstOrNull()?.emoji.orEmpty().ifEmpty { "🎉" },
                                    primaryLabel = successPrimary,
                                )
                            }
                        }
                    }
                    wrongCount >= HINT_AFTER_WRONGS -> showHint = true
                }
            }
        }
    }

    val gridAlpha by animateFloatAsState(
        targetValue = if (listenPlaying) 0.3f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "grid-alpha",
    )
    val gridScale by animateFloatAsState(
        targetValue = if (listenPlaying) 0.95f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "grid-scale",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    title = stringResource(Res.string.identify_title),
                    currentStepIndex = STEP_INDEX,
                    onClose = onClose,
                    onStepJump = onStepJump,
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
                TargetWordHeader(
                    targetText = target.text,
                    listenPlaying = listenPlaying,
                    onListen = {
                        scope.launch {
                            listenPlaying = true
                            delay(LISTEN_DURATION_MS)
                            listenPlaying = false
                        }
                    },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.identify_instruction),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                RoundProgressRow(currentRound = roundIndex, totalRounds = totalRounds)
                Spacer(Modifier.height(14.dp))
                IdentifyGrid(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(gridAlpha)
                        .scale(gridScale),
                    items = gridItems,
                    selectedText = selectedText,
                    correctText = target.text,
                    wiggleTarget = wiggleTarget,
                    wiggleKey = wiggleKey,
                    showHint = showHint,
                    autoRevealed = autoRevealed,
                    onSelect = onCardTap,
                )
                Spacer(Modifier.weight(1f, fill = true))
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
private fun TargetWordHeader(
    targetText: String,
    listenPlaying: Boolean,
    onListen: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = targetText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center,
        ) {
            PulseRings(
                isActive = listenPlaying,
                ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            )
            IconButton(
                onClick = onListen,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(Res.string.identify_listen_cd),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun RoundProgressRow(
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
            text = stringResource(Res.string.identify_round_progress, currentRound + 1, totalRounds),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun IdentifyGrid(
    items: ImmutableList<LessonWord>,
    selectedText: String?,
    correctText: String,
    wiggleTarget: String?,
    wiggleKey: Int,
    showHint: Boolean,
    autoRevealed: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.chunked(COLUMNS).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    IdentifyCard(
                        item = item,
                        selected = selectedText == item.text,
                        isCorrect = item.text == correctText,
                        wiggleActive = wiggleTarget == item.text,
                        wiggleKey = wiggleKey,
                        showHint = showHint,
                        autoRevealed = autoRevealed,
                        selectionLocked = selectedText != null || autoRevealed,
                        onClick = { onSelect(item.text) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (rowItems.size < COLUMNS) {
                    repeat(COLUMNS - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun IdentifyCard(
    item: LessonWord,
    selected: Boolean,
    isCorrect: Boolean,
    wiggleActive: Boolean,
    wiggleKey: Int,
    showHint: Boolean,
    autoRevealed: Boolean,
    selectionLocked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(wiggleKey, wiggleActive) {
        if (wiggleActive && wiggleKey > 0) {
            rotation.snapTo(0f)
            rotation.animateTo(-10f, tween(80))
            rotation.animateTo(10f, tween(80))
            rotation.animateTo(-6f, tween(80))
            rotation.animateTo(0f, tween(80))
        }
    }

    val showCorrectCheck = selected && isCorrect
    val showRevealed = autoRevealed && isCorrect
    val showHintGlow = showHint && isCorrect && !selected && !autoRevealed

    val hintTransition = rememberInfiniteTransition(label = "hint")
    val hintAlpha by hintTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hint-alpha",
    )

    val borderWidth = when {
        showCorrectCheck || showRevealed -> 3.dp
        else -> 1.dp
    }
    val borderColor = when {
        showCorrectCheck || showRevealed -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val shadowAlpha = when {
        showCorrectCheck || showRevealed -> 0.45f
        showHintGlow -> hintAlpha
        else -> 0.15f
    }
    val shadowTint = when {
        showHintGlow -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val cardShape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }

    Box(modifier = modifier.graphicsLayer { rotationZ = rotation.value }) {
        PuffySurface(
            modifier = Modifier
                .fillMaxWidth()
                .height(CARD_HEIGHT)
                .border(borderWidth, borderColor, cardShape)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = MaterialTheme.colorScheme.primary),
                    enabled = !selectionLocked,
                    onClick = onClick,
                ),
            shape = cardShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = if (showCorrectCheck || showRevealed) 14.dp else 8.dp,
            shadowTint = shadowTint,
            shadowAlpha = shadowAlpha,
            topHighlightHeight = 10.dp,
            topHighlightAlpha = 0.85f,
            bottomShadeHeight = 10.dp,
            bottomShadeAlpha = 0.08f,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.emoji.orEmpty().ifEmpty { "🎨" },
                    fontSize = 68.sp,
                )
            }
        }
        if (showCorrectCheck) {
            CheckBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
    }
}

@Composable
private fun CheckBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}

// -------- Helpers --------

private fun buildRoundGrid(
    target: LessonWord,
    sameLetterVocab: List<LessonWord>,
    otherWords: List<PhonicsLesson>,
    seed: Int,
    gridSize: Int = OPTIONS_COUNT,
): ImmutableList<LessonWord> {
    val rng = Random(seed)
    // Familiar slot: 1 other vocab from same letter (if available)
    val familiar = sameLetterVocab
        .filter { it.text != target.text }
        .shuffled(rng)
        .firstOrNull()
    // Decoy pool: vocab items of OTHER letters (or fallback to main word)
    val decoyPool = otherWords
        .flatMap { word ->
            word.words
        }
        .distinctBy { it.text }
        .filter { it.text != target.text && it.text != familiar?.text }
    val decoyCount = gridSize - 1 - (if (familiar != null) 1 else 0)
    val decoys = decoyPool.shuffled(rng).take(decoyCount)
    val combined = (listOfNotNull(target, familiar) + decoys)
        .distinctBy { it.text }
        .shuffled(rng)
    return combined.toImmutableList()
}

// -------- Constants --------

private const val COLUMNS = 2
private val CARD_HEIGHT = 120.dp
private const val OPTIONS_COUNT = 6
private const val LISTEN_DURATION_MS = 1000L
private const val CORRECT_CELEBRATION_MS = 800L
private const val AUTO_REVEAL_DELAY_MS = 1500L
private const val HINT_AFTER_WRONGS = 2
private const val MAX_WRONG_BEFORE_REVEAL = 3
