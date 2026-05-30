package me.ltthuc.kmp.feature.learningpath.step.story

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import me.ltthuc.kmp.core.resource.step_guide_story
import me.ltthuc.kmp.core.resource.story_audio_cd
import me.ltthuc.kmp.core.resource.story_next_page_cd
import me.ltthuc.kmp.core.resource.story_previous_page_cd
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.DEFAULT_VISIBLE_STEPS
import me.ltthuc.kmp.feature.learningpath.step.STORY_SEGMENT_INDEX
import me.ltthuc.kmp.feature.learningpath.step.common.KaraokeText
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val UNIT_STORY_STEP_INDEX = STORY_SEGMENT_INDEX // = 7
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
            navBackStack.add(Destination.Learning.UnitGame(levelId, unitId, gameIndex = 0))
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
                currentStepIndex = UNIT_STORY_STEP_INDEX,
                stepSegments = stepSegments,
                onClose = onClose,
                onStepJump = onStepJump,
                onNext = onNext,
                nextEnabled = true,
                guideText = stringResource(Res.string.step_guide_story),
                guideTrailing = {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PulseRings(
                            isActive = isNarrating,
                            ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        )
                        IconButton(
                            onClick = onListen,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(Res.string.story_audio_cd),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
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
            val positionMs = when (val s = audioState) {
                is AudioState.Playing -> s.positionMs
                is AudioState.Paused -> s.positionMs
                else -> -1L
            }
            KaraokeText(
                text = currentScene.text,
                isPlaying = isNarrating,
                positionMs = positionMs,
                wordTimings = currentScene.wordTimings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.weight(1f, fill = true))
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
        StoryStyleCard(aspectRatio = null, whiteInner = true) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
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

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun StorySceneCard(scene: StoryScene) {
    val imagePath = scene.imagePathLandscape
    val bitmap: ImageBitmap? = if (imagePath != null) {
        produceState<ImageBitmap?>(initialValue = null, imagePath) {
            value = runCatching { Res.readBytes(imagePath).decodeToImageBitmap() }
                .onFailure { Napier.w(tag = TAG) { "No image at $imagePath, falling back to emoji" } }
                .getOrNull()
        }.value
    } else {
        null
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = scene.text,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = sceneEmoji(scene.name),
                fontSize = 120.sp,
                textAlign = TextAlign.Center,
            )
        }
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
