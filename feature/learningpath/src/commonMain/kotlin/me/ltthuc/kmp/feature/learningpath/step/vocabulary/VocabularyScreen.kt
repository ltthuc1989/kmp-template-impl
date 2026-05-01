package me.ltthuc.kmp.feature.learningpath.step.vocabulary

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.chant_previous
import me.ltthuc.kmp.core.resource.score_fail_primary
import me.ltthuc.kmp.core.resource.score_fail_subtitle
import me.ltthuc.kmp.core.resource.score_fail_title
import me.ltthuc.kmp.core.resource.score_success_primary
import me.ltthuc.kmp.core.resource.score_success_subtitle
import me.ltthuc.kmp.core.resource.score_success_title
import me.ltthuc.kmp.core.resource.vocabulary_listen_cd
import me.ltthuc.kmp.core.resource.vocabulary_mic_cd
import me.ltthuc.kmp.core.resource.vocabulary_next_cd
import me.ltthuc.kmp.core.resource.vocabulary_previous_cd
import me.ltthuc.kmp.core.resource.vocabulary_title
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedback
import me.ltthuc.kmp.feature.learningpath.step.common.ScoreFeedbackOverlay
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StepNavRow
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.random.Random

private const val STEP_INDEX = 2

@Composable
internal fun VocabularyScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showSpeakButton: Boolean = false,
    totalSteps: Int = 7,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: VocabularyViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
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
        VocabularyContent(
            currentLesson = currentLesson,
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            audioState = audioState,
            onListenWord = { word -> viewModel.onListenWordToggle(currentLesson, word) },
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            showSpeakButton = showSpeakButton,
            totalSteps = totalSteps,
        )
    }
}

@Composable
private fun VocabularyContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    audioState: AudioState,
    onListenWord: (word: String) -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    showSpeakButton: Boolean = false,
    totalSteps: Int = 7,
) {
    val vocabItems = remember(currentLesson.id) {
        currentLesson.words.takeIf { it.isNotEmpty() }
            ?: listOf(LessonWord(word = currentLesson.displayLetter, emoji = null))
    }.toImmutableList()

    var vocabIndex by remember(currentLesson.id) { mutableStateOf(0) }
    var micRecording by remember(currentLesson.id) { mutableStateOf(false) }
    var scoreFeedback by remember { mutableStateOf<ScoreFeedback?>(null) }

    val safeVocabIndex = vocabIndex.coerceIn(0, vocabItems.lastIndex)
    val currentVocab = vocabItems[safeVocabIndex]
    val currentWordRef = remember(currentLesson.id, currentVocab.word) {
        currentLesson.wordRef(currentVocab.word)
    }
    val listenPlaying = currentWordRef != null && audioState.isActiveFor(currentWordRef)

    // Mic stub: auto-stop after 2s + random score result (replace with Gemini STT later)
    val successTitle = stringResource(Res.string.score_success_title)
    val successSubtitle = stringResource(Res.string.score_success_subtitle)
    val successPrimary = stringResource(Res.string.score_success_primary)
    val failTitle = stringResource(Res.string.score_fail_title)
    val failSubtitle = stringResource(Res.string.score_fail_subtitle)
    val failPrimary = stringResource(Res.string.score_fail_primary)
    LaunchedEffect(micRecording) {
        if (micRecording) {
            delay(MIC_DURATION_MS)
            micRecording = false
            scoreFeedback = if (Random.nextBoolean()) {
                ScoreFeedback.Success(
                    title = successTitle,
                    subtitle = successSubtitle,
                    heroEmoji = currentVocab.emoji.orEmpty().ifEmpty { "🎉" },
                    primaryLabel = successPrimary,
                )
            } else {
                ScoreFeedback.Fail(
                    title = failTitle,
                    subtitle = failSubtitle,
                    heroEmoji = "😔",
                    primaryLabel = failPrimary,
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                StepHeader(
                    title = stringResource(Res.string.vocabulary_title),
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
                VocabularyHeroCard(
                    vocab = currentVocab,
                    vocabIndex = safeVocabIndex,
                    vocabTotal = vocabItems.size,
                    listenPlaying = listenPlaying,
                    micRecording = micRecording,
                    showSpeakButton = showSpeakButton,
                    onVocabPrevious = { if (safeVocabIndex > 0) vocabIndex = safeVocabIndex - 1 },
                    onVocabNext = { if (safeVocabIndex < vocabItems.lastIndex) vocabIndex = safeVocabIndex + 1 },
                    onListenToggle = { onListenWord(currentVocab.word) },
                    onMicToggle = { micRecording = !micRecording },
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
            feedback = scoreFeedback,
            onDismiss = { scoreFeedback = null },
            onPrimary = {
                val wasSuccess = scoreFeedback is ScoreFeedback.Success
                scoreFeedback = null
                if (wasSuccess) onNext()
            },
        )
    }
}

@Composable
private fun VocabularyHeroCard(
    vocab: LessonWord,
    vocabIndex: Int,
    vocabTotal: Int,
    listenPlaying: Boolean,
    micRecording: Boolean,
    onVocabPrevious: () -> Unit,
    onVocabNext: () -> Unit,
    onListenToggle: () -> Unit,
    onMicToggle: () -> Unit,
    showSpeakButton: Boolean = false,
) {
    StoryStyleCard(aspectRatio = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top row with arrows + emoji centered
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                VocabArrowButton(
                    icon = Icons.Filled.ChevronLeft,
                    contentDescription = stringResource(Res.string.vocabulary_previous_cd),
                    enabled = vocabIndex > 0,
                    onClick = onVocabPrevious,
                )
                CharacterArtwork(emoji = vocab.emoji.orEmpty())
                VocabArrowButton(
                    icon = Icons.Filled.ChevronRight,
                    contentDescription = stringResource(Res.string.vocabulary_next_cd),
                    enabled = vocabIndex < vocabTotal - 1,
                    onClick = onVocabNext,
                )
            }
            if (vocabTotal > 1) {
                Spacer(Modifier.height(8.dp))
                VocabDots(currentIndex = vocabIndex, total = vocabTotal)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = vocab.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                AnimatedListenButton(isPlaying = listenPlaying, onClick = onListenToggle)
                if (showSpeakButton) {
                    AnimatedMicButton(isRecording = micRecording, onClick = onMicToggle)
                }
            }
        }
    }
}

@Composable
private fun VocabArrowButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            },
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun VocabDots(currentIndex: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                    ),
            )
        }
    }
}

@Composable
private fun CharacterArtwork(emoji: String) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji.ifEmpty { "🎨" },
            fontSize = 96.sp,
        )
    }
}

@Composable
private fun AnimatedListenButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center,
    ) {
        PulseRings(
            isActive = isPlaying,
            ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
        )
        val interaction = remember { MutableInteractionSource() }
        PuffySurface(
            modifier = Modifier
                .size(52.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = MaterialTheme.colorScheme.onSecondaryContainer, bounded = false),
                    onClick = onClick,
                ),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 8.dp,
            shadowTint = MaterialTheme.colorScheme.secondary,
            shadowAlpha = 0.7f,
            topHighlightHeight = 7.dp,
            topHighlightAlpha = 0.8f,
            bottomShadeHeight = 7.dp,
            bottomShadeAlpha = 0.15f,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(Res.string.vocabulary_listen_cd),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun AnimatedMicButton(isRecording: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val iconScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "icon-scale",
    )
    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center,
    ) {
        PulseRings(
            isActive = isRecording,
            ringColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        )
        val interaction = remember { MutableInteractionSource() }
        PuffySurface(
            modifier = Modifier
                .size(52.dp)
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = MaterialTheme.colorScheme.onPrimary, bounded = false),
                    onClick = onClick,
                ),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            shadowElevation = 10.dp,
            shadowTint = MaterialTheme.colorScheme.primary,
            shadowAlpha = 0.55f,
            topHighlightHeight = 7.dp,
            topHighlightAlpha = 0.3f,
            bottomShadeHeight = 7.dp,
            bottomShadeAlpha = 0.22f,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = stringResource(Res.string.vocabulary_mic_cd),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(if (isRecording) iconScale else 1f),
                )
            }
        }
    }
}

private const val MIC_DURATION_MS = 2000L
