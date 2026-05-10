package me.ltthuc.kmp.feature.learningpath.step.identify

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.chant_previous
import me.ltthuc.kmp.core.resource.identify_all_done_soft_subtitle
import me.ltthuc.kmp.core.resource.identify_all_done_subtitle
import me.ltthuc.kmp.core.resource.identify_all_done_title
import me.ltthuc.kmp.core.resource.identify_listen_cd
import me.ltthuc.kmp.core.resource.identify_title
import me.ltthuc.kmp.core.resource.score_success_primary
import me.ltthuc.kmp.core.resource.step_guide_identify
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef
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
    onHome: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: IdentifyViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

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
        IdentifyContent(
            currentLesson = currentLesson,
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            audioState = audioState,
            onPlayWord = { word -> viewModel.playTargetWord(currentLesson, word) },
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
private fun IdentifyContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    audioState: AudioState,
    onPlayWord: (word: String) -> Unit,
    onClose: () -> Unit,
    onHome: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
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
    var finalOverlay by remember(currentLesson.id) { mutableStateOf<ScoreFeedback?>(null) }
    var allRoundsCompleted by remember(currentLesson.id) { mutableStateOf(false) }
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

    val targetRef = remember(currentLesson.id, target.text) { currentLesson.wordRef(target.text) }
    val listenPlaying = targetRef != null && audioState.isActiveFor(targetRef)

    val successPrimary = stringResource(Res.string.score_success_primary)
    val allDoneTitle = stringResource(Res.string.identify_all_done_title)
    val allDoneSubtitle = stringResource(Res.string.identify_all_done_subtitle, totalRounds)
    val allDoneSoftSubtitle = stringResource(Res.string.identify_all_done_soft_subtitle)

    val scope = rememberCoroutineScope()

    // Auto-play target word on round change. Grid alpha follows audio state.
    LaunchedEffect(currentLesson.id, roundIndex) {
        onPlayWord(target.text)
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
                        allRoundsCompleted = true
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
                                allRoundsCompleted = true
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
                    onHomeClick = onHome,
                    onStepJump = onStepJump,
                    stepSegments = stepSegments,
                    guideText = stringResource(Res.string.step_guide_identify),
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
                TargetWordHeader(
                    targetText = target.text,
                    listenPlaying = listenPlaying,
                    onListen = { onPlayWord(target.text) },
                )
                Spacer(Modifier.height(8.dp))
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
                BottomBannerAd()
                Spacer(Modifier.height(8.dp))
                StepContinueButton(
                    label = stringResource(Res.string.chant_next),
                    onClick = onNext,
                    enabled = allRoundsCompleted && finalOverlay == null,
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
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            PulseRings(
                isActive = listenPlaying,
                ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
            )
            IconButton(
                onClick = onListen,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(Res.string.identify_listen_cd),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
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
                WordDisplayView(
                    word = item,
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
private const val CORRECT_CELEBRATION_MS = 800L
private const val AUTO_REVEAL_DELAY_MS = 1500L
private const val HINT_AFTER_WRONGS = 2
private const val MAX_WRONG_BEFORE_REVEAL = 3
