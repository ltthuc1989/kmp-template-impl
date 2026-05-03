package me.ltthuc.kmp.feature.learningpath.step.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActive
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.model.Story
import me.ltthuc.kmp.core.model.StoryScene
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_next
import me.ltthuc.kmp.core.resource.chant_previous
import me.ltthuc.kmp.core.resource.story_audio_cd
import me.ltthuc.kmp.core.resource.story_next_page_cd
import me.ltthuc.kmp.core.resource.story_previous_page_cd
import me.ltthuc.kmp.core.resource.story_title
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.DEFAULT_VISIBLE_STEPS
import me.ltthuc.kmp.feature.learningpath.step.STORY_SEGMENT_INDEX
import me.ltthuc.kmp.feature.learningpath.step.common.CircularAudioButton
import me.ltthuc.kmp.feature.learningpath.step.common.KaraokeText
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StepNavRow
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val UNIT_STORY_STEP_INDEX = STORY_SEGMENT_INDEX // = 7
private const val UNIT_COMPLETE_STARS = 24
private const val TAG = "StoryScreen"

@Composable
internal fun StoryScreen(
    levelId: String,
    unitId: String,
    modifier: Modifier = Modifier,
    viewModel: StoryViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val navBackStack = LocalNavBackStack.current
    val progressRepository: LearningProgressRepository = koinInject()
    val levelRepository: LevelRepository = koinInject()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    var visibleSteps by remember(levelId) { mutableStateOf(DEFAULT_VISIBLE_STEPS) }
    LaunchedEffect(levelId) {
        visibleSteps = levelRepository.getVisibleSteps(levelId)
    }
    val stepSegments = remember(visibleSteps) {
        (visibleSteps + STORY_SEGMENT_INDEX).toImmutableList()
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val totalLessons = uiState.lessons.size
        val lastLessonIndex = (totalLessons - 1).coerceAtLeast(0)
        val perLessonSteps = visibleSteps.size

        LaunchedEffect(levelId, unitId, totalLessons, perLessonSteps) {
            if (totalLessons > 0 && perLessonSteps > 0) {
                val unitStepsTotal = totalLessons * perLessonSteps + 1
                val completedSteps = totalLessons * perLessonSteps
                val progressPercent = (completedSteps * 100) / unitStepsTotal
                progressRepository.setActivePosition(
                    levelId = levelId,
                    unitId = unitId,
                    lessonIndex = lastLessonIndex,
                    stepIndex = UNIT_STORY_STEP_INDEX,
                    progressPercent = progressPercent,
                )
            }
        }

        val onClose: () -> Unit = {
            val bookIdx = navBackStack.indexOfLast { it is Destination.Learning.UnitSelection }
            if (bookIdx >= 0) {
                while (navBackStack.size > bookIdx + 1) navBackStack.removeAt(navBackStack.lastIndex)
            } else {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
        }
        val onPrevious: () -> Unit = {
            if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.lastIndex)
        }
        val onNext: () -> Unit = {
            navBackStack.add(
                Destination.Learning.UnitComplete(levelId, unitId, starsEarned = UNIT_COMPLETE_STARS),
            )
        }
        val onStepJump: (Int) -> Unit = { targetStep ->
            if (targetStep in visibleSteps) {
                Napier.d(tag = TAG) {
                    "Jump from UnitStory to Step($lastLessonIndex,$targetStep)"
                }
                if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                navBackStack.add(
                    Destination.Learning.Step(levelId, unitId, lastLessonIndex, targetStep),
                )
            }
        }

        StoryContent(
            lessons = uiState.lessons,
            story = uiState.story,
            scenes = uiState.scenes,
            audioState = audioState,
            onPageChange = viewModel::onPageChange,
            onListen = viewModel::onListenToggle,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

@Composable
private fun StoryContent(
    lessons: ImmutableList<PhonicsLesson>,
    story: Story,
    scenes: ImmutableList<StoryScene>,
    audioState: AudioState,
    onPageChange: (Int) -> Unit,
    onListen: () -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    if (scenes.isEmpty()) return

    val pageCount = scenes.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val currentPage by remember { derivedStateOf { pagerState.currentPage.coerceIn(0, scenes.lastIndex) } }
    val scope = rememberCoroutineScope()

    // Auto-play scene audio when user swipes/lands on a new page (including first entry).
    LaunchedEffect(currentPage) {
        onPageChange(currentPage)
    }

    val currentScene = scenes[currentPage]
    val isNarrating = audioState.isActive()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                title = stringResource(Res.string.story_title),
                currentStepIndex = UNIT_STORY_STEP_INDEX,
                stepSegments = stepSegments,
                onClose = onClose,
                onStepJump = onStepJump,
            )
        },
        bottomBar = {
            LetterStepperBar(
                lessons = lessons,
                currentIndex = 0,
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
                text = story.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            StorySceneImagePager(
                scenes = scenes,
                pagerState = pagerState,
                onPreviousPage = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage - 1).coerceAtLeast(0),
                        )
                    }
                },
                onNextPage = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(scenes.lastIndex),
                        )
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            PageDotsRow(currentPage = currentPage, total = pageCount)
            Spacer(Modifier.height(16.dp))
            KaraokeText(
                text = currentScene.text,
                isPlaying = isNarrating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(24.dp))
            CircularAudioButton(
                isPlaying = isNarrating,
                onClick = onListen,
                contentDescription = stringResource(Res.string.story_audio_cd),
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
}

@Composable
@Suppress("UnstableCollections")
private fun StorySceneImagePager(
    scenes: List<StoryScene>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val canPrev = pagerState.currentPage > 0
    val canNext = pagerState.currentPage < scenes.lastIndex

    Box(modifier = Modifier.fillMaxWidth()) {
        StoryStyleCard {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                StorySceneCard(scene = scenes[pageIndex])
            }
        }

        ChevronButton(
            icon = Icons.Filled.ChevronLeft,
            contentDescription = stringResource(Res.string.story_previous_page_cd),
            enabled = canPrev,
            onClick = onPreviousPage,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
        )
        ChevronButton(
            icon = Icons.Filled.ChevronRight,
            contentDescription = stringResource(Res.string.story_next_page_cd),
            enabled = canNext,
            onClick = onNextPage,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
        )
    }
}

@Composable
private fun StorySceneCard(scene: StoryScene) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Placeholder: emoji per scene name. Swap for real illustration when art lands.
        Text(
            text = sceneEmoji(scene.name),
            fontSize = 120.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun sceneEmoji(name: String): String = when (name.lowercase()) {
    "intro" -> "📖"
    "problem" -> "🤔"
    "solution" -> "💡"
    "ending" -> "✨"
    else -> "🎬"
}

@Composable
private fun ChevronButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }
    val borderColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(CHEVRON_SIZE_DP.dp)
            .shadow(elevation = 8.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(28.dp),
        )
    }
}

private const val CHEVRON_SIZE_DP = 48
