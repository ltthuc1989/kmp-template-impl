package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.UnitCard
import me.ltthuc.kmp.core.model.UnitLetterPreview
import me.ltthuc.kmp.core.model.UnitStatus
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitCompletionRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.unit_practice_chip
import me.ltthuc.kmp.core.resource.unit_practice_count
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenBg = Color(0xFFFCE4E7)
private val AccentRed = Color(0xFFE63946)
private val SoftPink = Color(0xFFF7B4BC)
private val LockedGray = Color(0xFFE4E7EB)
private val LockedTextGray = Color(0xFF9AA3AF)
private val PracticedChipBg = Color(0xFFD1F2DA)
private val PracticedChipText = Color(0xFF1F8A3F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitSelectionScreen(
    levelId: String,
    modifier: Modifier = Modifier,
    viewModel: UnitSelectionViewModel = koinViewModel(key = levelId) { parametersOf(levelId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current
    val progressRepository: LearningProgressRepository = koinInject()
    val levelRepository: LevelRepository = koinInject()
    val unitRepository: UnitRepository = koinInject()
    val unitCompletionRepository: UnitCompletionRepository = koinInject()
    val appSettingRepository: AppSettingRepository = koinInject()
    val appSetting by appSettingRepository.setting.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var sheetTarget by remember { mutableStateOf<UnitCard?>(null) }

    val isStartDestination = navBackStack.size == 1
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ScreenBg,
        topBar = {
            val uiState = (screenState as? ScreenState.Idle)?.data
            UnitTopBar(
                title = uiState?.let { "Book ${it.level.number}: ${it.level.title}" }.orEmpty(),
                startedCount = uiState?.startedCount ?: 0,
                totalUnits = uiState?.units?.size ?: 0,
                showBackButton = !isStartDestination,
                isMuted = appSetting.globalMuted,
                onToggleMute = {
                    scope.launch { appSettingRepository.setGlobalMuted(!appSetting.globalMuted) }
                },
                onBack = { if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.size - 1) },
                onSettings = { navBackStack.add(Destination.Setting.Root) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { BottomBannerAd() },
    ) { innerPadding ->
        AsyncLoadContents(
            modifier = Modifier.fillMaxSize(),
            screenState = screenState,
        ) { uiState ->
            UnitSelectionList(
                units = uiState.units,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                onUnitClick = { card ->
                    when (card.status) {
                        UnitStatus.Locked -> Unit
                        UnitStatus.Completed -> {
                            // Completed units open the lesson selector sheet so the kid can
                            // pick any letter (or Story) to replay independently.
                            sheetTarget = card
                        }
                        UnitStatus.Active, UnitStatus.Unlocked -> {
                            scope.launch {
                                // Resume from saved position if user last left this unit,
                                // else start fresh from lesson 0 first visible step.
                                val active = progressRepository.current()
                                val visibleSteps = levelRepository.getVisibleSteps(levelId)
                                val firstVisible = visibleSteps.firstOrNull() ?: 0
                                val startLesson = if (active?.activeUnitId == card.unit.id) {
                                    active.activeLessonIndex
                                } else {
                                    0
                                }
                                val startStep = if (active?.activeUnitId == card.unit.id) {
                                    val saved = active.activeStepIndex
                                    if (saved in visibleSteps) {
                                        saved
                                    } else {
                                        visibleSteps.firstOrNull { it >= saved } ?: firstVisible
                                    }
                                } else {
                                    firstVisible
                                }
                                navBackStack.add(
                                    Destination.Learning.Step(
                                        levelId = levelId,
                                        unitId = card.unit.id,
                                        lessonIndex = startLesson,
                                        stepIndex = startStep,
                                    ),
                                )
                            }
                        }
                    }
                },
            )
        }
    }

    sheetTarget?.let { card ->
        val lessons by remember(card.unit.id) {
            unitRepository.observeLessons(card.unit.id)
        }.collectAsStateWithLifecycle(initialValue = emptyList())

        LessonSelectorSheet(
            unit = card.unit,
            lessons = lessons.toImmutableList(),
            completionCount = card.completionCount,
            onLessonClick = { lessonIdx ->
                sheetTarget = null
                scope.launch {
                    val visibleSteps = levelRepository.getVisibleSteps(levelId)
                    val firstVisible = visibleSteps.firstOrNull() ?: 0
                    navBackStack.add(
                        Destination.Learning.Step(
                            levelId = levelId,
                            unitId = card.unit.id,
                            lessonIndex = lessonIdx,
                            stepIndex = firstVisible,
                        ),
                    )
                }
            },
            onStoryClick = {
                sheetTarget = null
                navBackStack.add(Destination.Learning.UnitStory(levelId, card.unit.id))
            },
            onRestart = {
                sheetTarget = null
                scope.launch {
                    unitCompletionRepository.reset(card.unit.id)
                    val visibleSteps = levelRepository.getVisibleSteps(levelId)
                    val firstVisible = visibleSteps.firstOrNull() ?: 0
                    progressRepository.setActivePosition(
                        levelId = levelId,
                        unitId = card.unit.id,
                        lessonIndex = 0,
                        stepIndex = firstVisible,
                        progressPercent = 0,
                    )
                    navBackStack.add(
                        Destination.Learning.Step(
                            levelId = levelId,
                            unitId = card.unit.id,
                            lessonIndex = 0,
                            stepIndex = firstVisible,
                        ),
                    )
                }
            },
            onDismiss = { sheetTarget = null },
        )
    }
}

@Composable
private fun UnitSelectionList(
    units: ImmutableList<UnitCard>,
    contentPadding: PaddingValues,
    onUnitClick: (UnitCard) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        itemsIndexed(
            items = units,
            key = { _, card -> card.unit.id },
        ) { index, card ->
            UnitRow(
                card = card,
                isFirst = index == 0,
                isLast = index == units.lastIndex,
                onClick = { onUnitClick(card) },
            )
        }
    }
}

@Composable
private fun UnitRow(
    card: UnitCard,
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
        TimelineColumn(
            status = card.status,
            unitNumber = card.unit.number,
            isFirst = isFirst,
            isLast = isLast,
            modifier = Modifier.fillMaxHeight(),
        )
        Spacer(Modifier.width(12.dp))
        UnitCardItem(
            card = card,
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = CardVerticalPadding),
        )
    }
}

@Composable
private fun TimelineColumn(
    status: UnitStatus,
    unitNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    val topLineColor = when {
        isFirst -> Color.Transparent
        status == UnitStatus.Completed || status == UnitStatus.Active -> AccentRed
        else -> LockedGray
    }
    val bottomLineColor = when {
        isLast -> Color.Transparent
        status == UnitStatus.Completed -> AccentRed
        else -> LockedGray
    }
    Box(
        modifier = modifier.width(TimelineColumnWidth),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(ConnectorStrokeWidth)
                    .background(topLineColor),
            )
            Spacer(Modifier.height(UnitBadgeSize))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(ConnectorStrokeWidth)
                    .background(bottomLineColor),
            )
        }
        UnitNumberBadge(status = status, unitNumber = unitNumber)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTopBar(
    title: String,
    startedCount: Int,
    totalUnits: Int,
    showBackButton: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    CenterAlignedTopAppBar(
        scrollBehavior = scrollBehavior,
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
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (totalUnits > 0) {
                StarBadge(count = startedCount, total = totalUnits)
                Spacer(Modifier.width(8.dp))
            }
            // Khan-style quick mute — one tap silences everything, persists across sessions.
            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (isMuted) {
                        Icons.AutoMirrored.Filled.VolumeOff
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = ScreenBg,
        ),
    )
}

@Composable
private fun StarBadge(count: Int, total: Int) {
    Surface(
        shape = CircleShape,
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = AccentRed,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "$count/$total",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AccentRed,
            )
        }
    }
}

@Composable
private fun UnitNumberBadge(status: UnitStatus, unitNumber: Int) {
    val label = unitNumber.toString().padStart(2, '0')
    val base = Modifier.size(UnitBadgeSize).clip(CircleShape)
    val styled = when (status) {
        UnitStatus.Completed -> base.background(AccentRed)
        UnitStatus.Active -> base.background(Color.White).border(2.dp, AccentRed, CircleShape)
        UnitStatus.Unlocked -> base.background(Color.White).border(1.5.dp, SoftPink, CircleShape)
        UnitStatus.Locked -> base.background(LockedGray)
    }
    Box(modifier = styled, contentAlignment = Alignment.Center) {
        when (status) {
            UnitStatus.Completed -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            UnitStatus.Locked -> Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = LockedTextGray,
                modifier = Modifier.size(14.dp),
            )
            UnitStatus.Active, UnitStatus.Unlocked -> Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentRed,
            )
        }
    }
}

@Composable
private fun UnitCardItem(
    card: UnitCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLocked = card.status == UnitStatus.Locked
    val containerColor = if (isLocked) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surface
    }

    val activeBorder = if (card.status == UnitStatus.Active) {
        Modifier.border(1.5.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(CardCornerRadius))
    } else {
        Modifier
    }

    ElevatedCard(
        onClick = onClick,
        enabled = !isLocked,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = CardMinHeight)
            .then(activeBorder),
        shape = RoundedCornerShape(CardCornerRadius),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            disabledContainerColor = containerColor,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isLocked) 0.dp else 3.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLocked) {
                    Text(
                        text = card.unit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = LockedTextGray,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        card.previewLetters.forEach { item ->
                            LetterPreview(item)
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                ActionPlayButton(status = card.status)
            }
            if (!isLocked && (card.unit.themeChip != null || card.completionCount > 0)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    card.unit.themeChip?.let { theme ->
                        ThemeChip(label = theme)
                    }
                    if (card.completionCount > 0) {
                        PracticedChip(count = card.completionCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun LetterPreview(item: UnitLetterPreview) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = item.letter.take(1),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (item.letter.length > 1) {
                Text(
                    text = item.letter.drop(1),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(text = item.emoji.orEmpty(), fontSize = 22.sp)
    }
}

@Composable
private fun ActionPlayButton(status: UnitStatus) {
    when (status) {
        UnitStatus.Completed -> Box(
            modifier = Modifier
                .size(PlayButtonSize)
                .clip(CircleShape)
                .border(1.5.dp, AccentRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Completed",
                tint = AccentRed,
                modifier = Modifier.size(18.dp),
            )
        }
        UnitStatus.Active, UnitStatus.Unlocked -> Box(
            modifier = Modifier
                .size(PlayButtonSize)
                .clip(CircleShape)
                .border(1.5.dp, AccentRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = AccentRed,
                modifier = Modifier.size(18.dp),
            )
        }
        UnitStatus.Locked -> Spacer(Modifier.size(PlayButtonSize))
    }
}

@Composable
private fun ThemeChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun PracticedChip(
    count: Int,
    modifier: Modifier = Modifier,
) {
    val accessibleLabel = stringResource(Res.string.unit_practice_count, count)
    Box(
        modifier = modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(PracticedChipBg)
            .padding(horizontal = 12.dp)
            .semantics { contentDescription = accessibleLabel },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.unit_practice_chip, count),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PracticedChipText,
        )
    }
}

private val UnitBadgeSize = 32.dp
private val PlayButtonSize = 34.dp
private val TimelineColumnWidth = 36.dp
private val ConnectorStrokeWidth = 2.dp
private val CardVerticalPadding = 6.dp
private val CardMinHeight = 76.dp
private val CardCornerRadius = 20.dp
