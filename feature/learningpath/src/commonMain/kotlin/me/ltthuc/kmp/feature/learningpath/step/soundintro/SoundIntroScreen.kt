package me.ltthuc.kmp.feature.learningpath.step.soundintro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.LessonWord
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.sound_intro_listen
import me.ltthuc.kmp.core.ui.audio.rememberAudioSession
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.WordDisplayView
import me.ltthuc.kmp.feature.learningpath.step.common.letterPair
import me.ltthuc.kmp.feature.learningpath.step.common.step0Refs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val STEP_INDEX = 0
private const val SOUND_GUIDE_MAX_MS = 6_000L

@Composable
internal fun SoundIntroScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: SoundIntroViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val guideAudio = rememberAudioSession()
    val lang = LocalAppLanguage.current

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
        // Phát guide nói trước, guide xong thì tự động phát audio sound của chữ.
        LaunchedEffect(currentLesson.id) {
            guideAudio.playAndAwait(AudioRef.Prompt("vp_step_sound", lang), SOUND_GUIDE_MAX_MS)
            viewModel.onListenToggle(currentLesson)
        }
        SoundIntroContent(
            currentLesson = currentLesson,
            audioState = audioState,
            onListen = { viewModel.onListenToggle(currentLesson) },
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

@Composable
private fun SoundIntroContent(
    currentLesson: PhonicsLesson,
    audioState: AudioState,
    onListen: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    val featuredWord = currentLesson.words.firstOrNull()
    // Level 2+ phát một chuỗi nhiều file, nên "đang phát" là ref hiện tại thuộc chuỗi
    // của lesson — không so được bằng một ref duy nhất.
    val refs = remember(currentLesson.id) { currentLesson.step0Refs() }
    val isPlaying = refs.any { audioState.isActiveFor(it) }

    // Gate Next button: kid must tap Listen at least once before advancing.
    var hasStartedListening by remember(currentLesson.id) { mutableStateOf(false) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) hasStartedListening = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                currentStepIndex = STEP_INDEX,
                onClose = onClose,
                onStepJump = onStepJump,
                stepSegments = stepSegments,
            )
        },
        bottomBar = {
            StepContinueButton(
                label = stringResource(Res.string.common_next),
                onClick = onNext,
                enabled = hasStartedListening,
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
            // Audio (listen) button sits on top of the letter, +30% size.
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                PulseRings(
                    isActive = isPlaying,
                    ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                )
                IconButton(
                    onClick = onListen,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(Res.string.sound_intro_listen),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = currentLesson.letterPair(),
                fontFamily = LocalPhonicsFontFamily.current,
                fontSize = 54.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            StoryStyleCard {
                FrameContent(item = featuredWord)
            }
            Spacer(Modifier.weight(1f, fill = true))
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
        WordDisplayView(
            word = item,
            fontSize = 120.sp,
        )
    }
}
