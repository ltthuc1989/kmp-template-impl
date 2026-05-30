package me.ltthuc.kmp.feature.learningpath.step.vocabulary

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.step_guide_vocabulary
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.common.wordRef
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val STEP_INDEX = 2

@Composable
internal fun VocabularyScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
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
            stepSegments = stepSegments,
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
    stepSegments: ImmutableList<Int>,
) {
    val vocabItems = remember(currentLesson.id) {
        currentLesson.words.takeIf { it.isNotEmpty() }
            ?: listOf(LessonWord(word = currentLesson.displayLetter, displays = emptyList()))
    }.toImmutableList()

    var heardWords by remember(currentLesson.id) { mutableStateOf(emptySet<String>()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                currentStepIndex = STEP_INDEX,
                onClose = onClose,
                onStepJump = onStepJump,
                onNext = onNext,
                nextEnabled = vocabItems.isNotEmpty() && heardWords.size >= vocabItems.size,
                stepSegments = stepSegments,
                guideText = stringResource(Res.string.step_guide_vocabulary),
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
            Spacer(Modifier.height(4.dp))
            VocabularyGrid(
                items = vocabItems,
                isPlaying = { word ->
                    val ref = currentLesson.wordRef(word) ?: return@VocabularyGrid false
                    audioState.isActiveFor(ref)
                },
                isHeard = { word -> word in heardWords },
                onListenWord = { word ->
                    heardWords = heardWords + word
                    onListenWord(word)
                },
                modifier = Modifier.weight(1f, fill = true),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun VocabularyGrid(
    items: ImmutableList<LessonWord>,
    isPlaying: (String) -> Boolean,
    isHeard: (String) -> Boolean,
    onListenWord: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(GRID_GAP_DP.dp),
        verticalArrangement = Arrangement.spacedBy(GRID_GAP_DP.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(items.size) { idx ->
            val word = items[idx]
            VocabularyGridCell(
                word = word,
                isPlaying = isPlaying(word.word),
                isHeard = isHeard(word.word),
                onClick = { onListenWord(word.word) },
            )
        }
    }
}

@Composable
private fun VocabularyGridCell(
    word: LessonWord,
    isPlaying: Boolean,
    isHeard: Boolean,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "vocab-cell-pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f,
        targetValue = PULSE_TARGET_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = PULSE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val cellScale = if (isPlaying) pulseScale else 1f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(cellScale),
    ) {
        StoryStyleCard(aspectRatio = 1f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WordDisplayView(
                        word = word,
                        fontSize = EMOJI_FONT_SP.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = word.word,
                        fontSize = WORD_FONT_SP.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        if (isHeard) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(BADGE_SIZE_DP.dp),
            )
        }
    }
}

private const val GRID_GAP_DP = 12
private const val EMOJI_FONT_SP = 56
private const val WORD_FONT_SP = 20
private const val BADGE_SIZE_DP = 24
private const val PULSE_TARGET_SCALE = 1.04f
private const val PULSE_DURATION_MS = 700
