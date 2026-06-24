package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.LessonCard
import me.ltthuc.kmp.core.model.LessonStatus
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.core.repository.LessonProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.lesson_map_games_label
import me.ltthuc.kmp.core.resource.lesson_sheet_lesson_label
import me.ltthuc.kmp.core.resource.lesson_sheet_story_label
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.core.ui.components.PuffySurface
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AccentRedSheet = Color(0xFFE63946)
private val LockedGraySheet = Color(0xFFE4E7EB)
private val LockedTextGraySheet = Color(0xFF9AA3AF)
private val LessonBadgeSize = 48.dp
private val LessonTimelineWidth = 56.dp
private val ConnectorStroke = 2.dp
// Keep the pulse rings inside the timeline column so they don't overlap the lesson card.
private const val TIMELINE_PULSE_MAX_SCALE = 1.35f
private const val LOCKED_ROW_ALPHA = 0.5f

/** Sentinel lessonId used in LessonProgress to mark a unit's Story as read (gates Mini Games). */
internal const val STORY_PROGRESS_ID = "__story__"

/**
 * Full-screen Lesson Map for a unit. Lists each lesson with a per-lesson lock state
 * (only lesson 1 unlocked by default; finishing a lesson unlocks the next), plus a
 * Story row and a Mini Games row that stay locked until every lesson is completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LessonMapScreen(
    levelId: String,
    unitId: String,
    modifier: Modifier = Modifier,
    viewModel: LessonMapViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current
    val levelRepository: LevelRepository = koinInject()
    val lessonProgressRepository: LessonProgressRepository = koinInject()
    val sfx: SfxController = koinInject()
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    // Tapping a locked row speaks the "finish the previous one first" guide (vp_locked).
    val onLockedTap = { sfx.playPrompt("vp_locked", lang) }

    // Mini Games unlock only after the Story has been read (in addition to all lessons done).
    val storyDone by remember(unitId) {
        lessonProgressRepository.observeByUnit(unitId).map { list ->
            list.any { it.lessonId == STORY_PROGRESS_ID && it.completionCount > 0 }
        }
    }.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            val title = (screenState as? ScreenState.Idle)?.data?.unit?.title.orEmpty()
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.lastIndex)
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                // See-through bar: zero-alpha but surface-based (NOT Color.Transparent, which is
                // transparent black). Both states share alpha 0, so the cross-fade stays fully
                // transparent — content shows through and there's no black flash on scroll.
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                ),
            )
        },
        bottomBar = { BottomBannerAd() },
    ) { innerPadding ->
        AsyncLoadContents(
            modifier = Modifier.fillMaxSize(),
            screenState = screenState,
            containerColor = Color.Transparent,
        ) { uiState ->
            LessonMapList(
                lessons = uiState.lessons,
                allLessonsComplete = uiState.allLessonsComplete,
                storyDone = storyDone,
                unitDone = uiState.completionCount > 0,
                onLockedTap = onLockedTap,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                    start = 20.dp,
                    end = 20.dp,
                ),
                onLessonClick = { card ->
                    scope.launch {
                        val firstVisible = levelRepository.getVisibleSteps(levelId).firstOrNull() ?: 0
                        navBackStack.add(
                            Destination.Learning.Step(
                                levelId = levelId,
                                unitId = unitId,
                                lessonIndex = card.lesson.orderIndex,
                                stepIndex = firstVisible,
                            ),
                        )
                    }
                },
                onStoryClick = {
                    navBackStack.add(Destination.Learning.UnitStory(levelId, unitId))
                },
                onGamesClick = {
                    navBackStack.add(Destination.Learning.UnitGame(levelId, unitId, 0))
                },
            )
        }
    }
}

@Composable
private fun LessonMapList(
    lessons: ImmutableList<LessonCard>,
    allLessonsComplete: Boolean,
    storyDone: Boolean,
    unitDone: Boolean,
    contentPadding: PaddingValues,
    onLessonClick: (LessonCard) -> Unit,
    onStoryClick: () -> Unit,
    onGamesClick: () -> Unit,
    onLockedTap: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(
            items = lessons,
            key = { _, card -> card.lesson.id },
        ) { index, card ->
            LessonRow(
                status = card.status,
                label = stringResource(
                    Res.string.lesson_sheet_lesson_label,
                    index + 1,
                    card.lesson.displayLetter,
                ),
                emoji = card.lesson.words.firstOrNull()?.emoji.orEmpty().ifEmpty { "📘" },
                isFirst = index == 0,
                isLast = false,
                onClick = {
                    if (card.status == LessonStatus.Locked) onLockedTap() else onLessonClick(card)
                },
            )
        }

        item(key = "story") {
            LessonRow(
                status = when {
                    storyDone -> LessonStatus.Completed
                    allLessonsComplete -> LessonStatus.Unlocked
                    else -> LessonStatus.Locked
                },
                label = stringResource(Res.string.lesson_sheet_story_label),
                emoji = "📖",
                isFirst = false,
                isLast = false,
                onClick = { if (allLessonsComplete) onStoryClick() else onLockedTap() },
            )
        }

        item(key = "games") {
            // Games unlock after lessons + story done; become Completed (✓, no focus) once the
            // whole unit is finished. Clickable whenever not Locked (replay like lessons).
            val gamesStatus = when {
                unitDone -> LessonStatus.Completed
                allLessonsComplete && storyDone -> LessonStatus.Unlocked
                else -> LessonStatus.Locked
            }
            LessonRow(
                status = gamesStatus,
                label = stringResource(Res.string.lesson_map_games_label),
                emoji = "🎮",
                isFirst = false,
                isLast = true,
                onClick = { if (gamesStatus != LessonStatus.Locked) onGamesClick() else onLockedTap() },
            )
        }
    }
}

@Composable
private fun LessonRow(
    status: LessonStatus,
    label: String,
    emoji: String,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top,
    ) {
        LessonTimelineColumn(
            status = status,
            isFirst = isFirst,
            isLast = isLast,
            modifier = Modifier.fillMaxHeight(),
        )
        Spacer(Modifier.width(12.dp))
        LessonCardItem(
            status = status,
            label = label,
            emoji = emoji,
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun LessonTimelineColumn(
    status: LessonStatus,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val done = status == LessonStatus.Completed
    val topLineColor = when {
        isFirst -> Color.Transparent
        // Incoming line is colored for any "reached" node (matches the badge's onRed logic), so the
        // path into the Unlocked frontier (e.g. Story after all lessons done) isn't half-gray.
        done || status == LessonStatus.Active || status == LessonStatus.Unlocked -> AccentRedSheet
        else -> LockedGraySheet
    }
    val bottomLineColor = when {
        isLast -> Color.Transparent
        done -> AccentRedSheet
        else -> LockedGraySheet
    }
    Box(
        modifier = modifier.width(LessonTimelineWidth),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.weight(1f).width(ConnectorStroke).background(topLineColor))
            Spacer(Modifier.height(LessonBadgeSize))
            Box(modifier = Modifier.weight(1f).width(ConnectorStroke).background(bottomLineColor))
        }
        LessonCircleBadge(status = status)
    }
}

@Composable
private fun LessonCircleBadge(status: LessonStatus) {
    // Frontier (Active/Unlocked) + Completed all render on red; only Locked is grey/white.
    val onRed = status == LessonStatus.Active ||
        status == LessonStatus.Unlocked ||
        status == LessonStatus.Completed
    val container = when {
        onRed -> AccentRedSheet
        status == LessonStatus.Locked -> LockedGraySheet
        else -> Color.White
    }
    Box(contentAlignment = Alignment.Center) {
        // Ripple pulse on the current lesson (the one to play next): the in-progress Active one,
        // or the next Unlocked frontier when nothing is mid-progress.
        // Smaller maxScale keeps the rings within the timeline column, off the lesson card.
        PulseRings(
            isActive = status == LessonStatus.Active || status == LessonStatus.Unlocked,
            ringColor = AccentRedSheet,
            maxScale = TIMELINE_PULSE_MAX_SCALE,
        )
        PuffySurface(
            shape = CircleShape,
            containerColor = container,
            shadowElevation = 8.dp,
            shadowTint = if (onRed) AccentRedSheet else LockedTextGraySheet,
            shadowAlpha = 0.40f,
            topHighlightHeight = 14.dp,
            topHighlightAlpha = 0.55f,
            bottomShadeHeight = 14.dp,
            bottomShadeAlpha = 0.16f,
            modifier = Modifier.size(LessonBadgeSize),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (status) {
                    LessonStatus.Completed -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    LessonStatus.Locked -> Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = LockedTextGraySheet,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonCardItem(
    status: LessonStatus,
    label: String,
    emoji: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = status == LessonStatus.Locked
    val shape = RoundedCornerShape(12.dp)
    // Card surface (shadow + bg) full opacity so the 3D lift shows even when locked; dim content only.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = if (locked) 5.dp else 9.dp, shape = shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            // Always clickable: a locked tap triggers the spoken guide instead of navigating.
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (locked) LOCKED_ROW_ALPHA else 1f)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
