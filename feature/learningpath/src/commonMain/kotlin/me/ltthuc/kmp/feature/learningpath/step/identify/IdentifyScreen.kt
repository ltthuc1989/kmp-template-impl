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
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.repository.AudioRepository
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.playAndAwait
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.identify_listen_cd
import me.ltthuc.kmp.core.resource.step_guide_identify
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private const val STEP_INDEX = 3

@Composable
internal fun IdentifyScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
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
            audioState = audioState,
            onPlayWord = { word -> viewModel.playTargetWord(currentLesson, word) },
            onClose = onClose,
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
    audioState: AudioState,
    onPlayWord: (word: String) -> Unit,
    onClose: () -> Unit,
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
    var allRoundsCompleted by remember(currentLesson.id) { mutableStateOf(false) }

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

    val scope = rememberCoroutineScope()
    val sfx = koinInject<SfxController>()
    val audioRepository = koinInject<AudioRepository>()
    val lang = LocalAppLanguage.current

    // First entry plays the spoken guide ("Listen, then tap the right picture"), then the target
    // word. Later rounds just play the word (after a short delay that lets the previous step's
    // audioRepository.stop() finish so our play() doesn't get cancelled).
    LaunchedEffect(currentLesson.id, roundIndex) {
        // The leading delay is required on EVERY branch: when we arrive from the previous
        // step its DisposableEffect runs audioRepository.stop() in dispose, and the guide
        // shares that same channel. Without the beat, play() races the outgoing stop() and
        // gets cancelled — the guide goes silent on first entry. (Was the missing-guide bug.)
        delay(AUTO_PLAY_DELAY_MS)
        if (roundIndex == 0) {
            audioRepository.playAndAwait(AudioRef.Prompt("vp_step_identify", lang), IDENTIFY_GUIDE_MAX_MS)
        }
        onPlayWord(target.text)
    }

    val onCardTap: (String) -> Unit = { tappedText ->
        // After auto-reveal: only the correct card is tappable (decoys ignored). Kid must
        // tap the highlighted answer themselves — matches Duolingo ABC / Khan Kids pattern
        // of preserving agency rather than auto-skipping.
        if (selectedText == null && !listenPlaying) {
            if (tappedText == target.text) {
                selectedText = tappedText
                // Only the "correct" chime plays here — no voice praise, no other SFX.
                sfx.playSfx("correct")
                scope.launch {
                    delay(CORRECT_CELEBRATION_MS)
                    selectedText = null
                    if (roundIndex < totalRounds - 1) {
                        roundIndex++
                    } else {
                        // Done: confetti effect + Next enabled, no popup, no completion sound.
                        allRoundsCompleted = true
                    }
                }
            } else if (!autoRevealed) {
                wrongCount++
                wiggleTarget = tappedText
                wiggleKey++
                // Khan-simple: NO error/voice SFX on a miss — visual shake is the only feedback.
                when {
                    wrongCount >= MAX_WRONG_BEFORE_REVEAL -> {
                        // Highlight correct — but DO NOT auto-skip. Kid taps the revealed
                        // answer themselves to advance.
                        autoRevealed = true
                    }
                    wrongCount >= HINT_AFTER_WRONGS -> showHint = true
                }
            }
            // else: tap on decoy after reveal — ignored
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
                    currentStepIndex = STEP_INDEX,
                    onClose = onClose,
                    onStepJump = onStepJump,
                    stepSegments = stepSegments,
                    guideText = stringResource(Res.string.step_guide_identify),
                    guideTrailing = {
                        Box(
                            modifier = Modifier.size(LISTEN_BOX_DP.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            PulseRings(
                                isActive = listenPlaying,
                                ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            )
                            IconButton(
                                onClick = { onPlayWord(target.text) },
                                modifier = Modifier.size(LISTEN_BUTTON_DP.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = stringResource(Res.string.identify_listen_cd),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(LISTEN_ICON_DP.dp),
                                )
                            }
                        }
                    },
                )
            },
            bottomBar = {
                StepContinueButton(
                    label = stringResource(Res.string.common_next),
                    onClick = onNext,
                    enabled = allRoundsCompleted,
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
                TargetWordHeader(targetText = target.text)
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
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TargetWordHeader(targetText: String) {
    Text(
        text = targetText,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
    )
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
                    val isCorrectItem = item.text == correctText
                    IdentifyCard(
                        item = item,
                        selected = selectedText == item.text,
                        isCorrect = isCorrectItem,
                        wiggleActive = wiggleTarget == item.text,
                        wiggleKey = wiggleKey,
                        showHint = showHint,
                        autoRevealed = autoRevealed,
                        // After auto-reveal: only the correct card stays tappable so kid
                        // taps the highlighted answer themselves (Option A — preserve agency).
                        selectionLocked = selectedText != null || (autoRevealed && !isCorrectItem),
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
private const val AUTO_PLAY_DELAY_MS = 500L
private const val IDENTIFY_GUIDE_MAX_MS = 6_000L
private const val HINT_AFTER_WRONGS = 2
private const val MAX_WRONG_BEFORE_REVEAL = 3

// Header listen icon — ~30% larger than the original 36/32/20dp.
private const val LISTEN_BOX_DP = 48
private const val LISTEN_BUTTON_DP = 48
private const val LISTEN_ICON_DP = 34
