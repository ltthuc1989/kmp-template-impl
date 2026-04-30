package me.matsumo.grabee.feature.learningpath.step.story

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.matsumo.grabee.core.model.PhonicsLesson
import me.matsumo.grabee.core.repository.LearningProgressRepository
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.chant_next
import me.matsumo.grabee.core.resource.chant_previous
import me.matsumo.grabee.core.resource.story_audio_cd
import me.matsumo.grabee.core.resource.story_letter_title
import me.matsumo.grabee.core.resource.story_next_page_cd
import me.matsumo.grabee.core.resource.story_page_sentence
import me.matsumo.grabee.core.resource.story_previous_page_cd
import me.matsumo.grabee.core.resource.story_title
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack
import me.matsumo.grabee.feature.learningpath.step.common.CircularAudioButton
import me.matsumo.grabee.feature.learningpath.step.common.KaraokeText
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PageDotsRow
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import me.matsumo.grabee.feature.learningpath.step.common.StepNavRow
import me.matsumo.grabee.feature.learningpath.step.common.StoryStyleCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val UNIT_STORY_STEP_INDEX = 7
private const val UNIT_STORY_TOTAL_STEPS = 8
private const val PER_LESSON_STEPS = 7
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
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val totalLessons = uiState.lessons.size
        val lastLessonIndex = (totalLessons - 1).coerceAtLeast(0)

        LaunchedEffect(levelId, unitId, totalLessons) {
            if (totalLessons > 0) {
                val unitStepsTotal = totalLessons * PER_LESSON_STEPS + 1
                val completedSteps = totalLessons * PER_LESSON_STEPS
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
            if (targetStep in 0 until UNIT_STORY_STEP_INDEX) {
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
            pages = uiState.pages,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
        )
    }
}

@Composable
private fun StoryContent(
    lessons: ImmutableList<PhonicsLesson>,
    pages: ImmutableList<StoryPage>,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
) {
    if (pages.isEmpty()) return

    val pageCount = pages.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val currentPage by remember { derivedStateOf { pagerState.currentPage.coerceIn(0, pages.lastIndex) } }
    val scope = rememberCoroutineScope()

    val currentStoryPage = pages[currentPage]
    val letterChar = currentStoryPage.letter
    val letterUpper = letterChar.uppercaseChar().toString()
    val letterLower = letterChar.lowercaseChar().toString()
    val currentLessonIndex = remember(letterChar, lessons) {
        lessons.indexOfFirst { it.displayLetter.firstOrNull() == letterChar }.coerceAtLeast(0)
    }

    var isNarrating by remember(currentPage) { mutableStateOf(false) }
    LaunchedEffect(currentPage, isNarrating) {
        if (isNarrating) {
            delay(NARRATION_STUB_MS)
            isNarrating = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                title = stringResource(Res.string.story_title),
                currentStepIndex = UNIT_STORY_STEP_INDEX,
                onClose = onClose,
                onStepJump = onStepJump,
                totalSteps = UNIT_STORY_TOTAL_STEPS,
            )
        },
        bottomBar = {
            LetterStepperBar(
                lessons = lessons,
                currentIndex = currentLessonIndex,
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
                text = stringResource(Res.string.story_letter_title, letterUpper, letterLower),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            StoryImagePager(
                pages = pages,
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
                            (pagerState.currentPage + 1).coerceAtMost(pages.lastIndex),
                        )
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            PageDotsRow(currentPage = currentPage, total = pageCount)
            Spacer(Modifier.height(16.dp))
            KaraokeText(
                text = stringResource(
                    Res.string.story_page_sentence,
                    letterUpper,
                    currentStoryPage.word.text,
                ),
                isPlaying = isNarrating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(24.dp))
            CircularAudioButton(
                isPlaying = isNarrating,
                onClick = { isNarrating = !isNarrating },
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
private fun StoryImagePager(
    pages: List<StoryPage>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val canPrev = pagerState.currentPage > 0
    val canNext = pagerState.currentPage < pages.lastIndex

    Box(modifier = Modifier.fillMaxWidth()) {
        StoryStyleCard {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { pageIndex ->
                StoryPageCard(item = pages[pageIndex])
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
private fun StoryPageCard(item: StoryPage) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.word.emoji.orEmpty().ifEmpty { "📖" },
            fontSize = 120.sp,
            textAlign = TextAlign.Center,
        )
    }
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

private const val NARRATION_STUB_MS = 2_400L
private const val CHEVRON_SIZE_DP = 48
