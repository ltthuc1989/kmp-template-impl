package me.ltthuc.kmp.feature.learningpath.step.soundintro

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.sound_intro_listen
import me.ltthuc.kmp.core.resource.sound_intro_next_lesson
import me.ltthuc.kmp.core.resource.sound_intro_title
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.CircularAudioButton
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.letterPair
import me.ltthuc.kmp.feature.learningpath.step.common.soundIntroRef
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val STEP_INDEX = 0

@Composable
internal fun SoundIntroScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    modifier: Modifier = Modifier,
    totalSteps: Int = 7,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: SoundIntroViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
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
        SoundIntroContent(
            currentLesson = currentLesson,
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            audioState = audioState,
            onListen = { viewModel.onListenToggle(currentLesson) },
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalSteps,
        )
    }
}

@Composable
private fun SoundIntroContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    audioState: AudioState,
    onListen: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    totalSteps: Int,
) {
    val featuredWord = currentLesson.words.firstOrNull()
    val ref = remember(currentLesson.id) { currentLesson.soundIntroRef() }
    val isPlaying = ref != null && audioState.isActiveFor(ref)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                title = stringResource(Res.string.sound_intro_title),
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
            Text(
                text = currentLesson.letterPair(),
                fontSize = 76.sp,
                lineHeight = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(12.dp))
            StoryStyleCard {
                FrameContent(item = featuredWord)
            }
            Spacer(Modifier.height(16.dp))
            CircularAudioButton(
                isPlaying = isPlaying,
                onClick = onListen,
                contentDescription = stringResource(Res.string.sound_intro_listen),
            )
            Spacer(Modifier.weight(1f, fill = true))
            NextLessonButton(onClick = onNext)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FrameContent(item: LessonWord?) {
    if (item == null) return
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // TODO render imageAsset when asset pipeline is online; emoji is the current placeholder.
        Text(
            text = item.emoji.orEmpty().ifEmpty { "🐝" },
            fontSize = 120.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NextLessonButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 14.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.55f,
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
            Text(
                text = stringResource(Res.string.sound_intro_next_lesson),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
