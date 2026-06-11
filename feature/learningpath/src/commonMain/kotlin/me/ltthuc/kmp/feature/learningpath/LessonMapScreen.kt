package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.LessonCard
import me.ltthuc.kmp.core.model.LessonStatus
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.lesson_map_games_label
import me.ltthuc.kmp.core.resource.lesson_sheet_lesson_label
import me.ltthuc.kmp.core.resource.lesson_sheet_story_label
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AccentRedSheet = Color(0xFFE63946)
private const val LOCKED_ROW_ALPHA = 0.5f

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
    val scope = rememberCoroutineScope()

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
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
                completionCount = uiState.completionCount,
                allLessonsComplete = uiState.allLessonsComplete,
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
    completionCount: Int,
    allLessonsComplete: Boolean,
    contentPadding: PaddingValues,
    onLessonClick: (LessonCard) -> Unit,
    onStoryClick: () -> Unit,
    onGamesClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = lessons,
            key = { _, card -> card.lesson.id },
        ) { index, card ->
            val locked = card.status == LessonStatus.Locked
            LessonRow(
                label = stringResource(
                    Res.string.lesson_sheet_lesson_label,
                    index + 1,
                    card.lesson.displayLetter,
                ),
                emoji = card.lesson.words.firstOrNull()?.emoji.orEmpty().ifEmpty { "📘" },
                locked = locked,
                completed = card.status == LessonStatus.Completed,
                onClick = { if (!locked) onLessonClick(card) },
            )
        }

        item(key = "story") {
            LessonRow(
                label = stringResource(Res.string.lesson_sheet_story_label),
                emoji = "📖",
                locked = !allLessonsComplete,
                completed = false,
                onClick = { if (allLessonsComplete) onStoryClick() },
            )
        }

        item(key = "games") {
            LessonRow(
                label = stringResource(Res.string.lesson_map_games_label),
                emoji = "🎮",
                locked = !allLessonsComplete,
                completed = false,
                onClick = { if (allLessonsComplete) onGamesClick() },
            )
        }
    }
}

@Composable
private fun LessonRow(
    label: String,
    emoji: String,
    locked: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(enabled = !locked, onClick = onClick)
            .alpha(if (locked) LOCKED_ROW_ALPHA else 1f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
        TrailingStatusIcon(locked = locked, completed = completed)
    }
}

@Composable
private fun TrailingStatusIcon(locked: Boolean, completed: Boolean) {
    when {
        locked -> Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        completed -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = AccentRedSheet,
            modifier = Modifier.size(26.dp),
        )
        else -> Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = AccentRedSheet,
            modifier = Modifier.size(28.dp),
        )
    }
}
