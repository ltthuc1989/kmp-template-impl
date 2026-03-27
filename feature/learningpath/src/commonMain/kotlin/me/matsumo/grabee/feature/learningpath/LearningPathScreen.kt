package me.matsumo.grabee.feature.learningpath

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun LearningPathScreen(
    modifier: Modifier = Modifier,
    viewModel: LearningPathViewModel = koinViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier,
        screenState = screenState,
        containerColor = LpColors.background,
        retryAction = viewModel::fetch,
    ) { uiState ->
        LearningPathContent(
            uiState = uiState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LearningPathContent(
    uiState: LearningPathUiState,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        containerColor = LpColors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Learning Path",
                        color = LpColors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = LpColors.onSurface,
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LpColors.secondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = LpColors.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LpColors.background,
                ),
            )
        },
        bottomBar = {
            LearningPathBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                ActiveJourneySection(level = uiState.activeLevel)
            }
            item {
                UnitsTimeline(level = uiState.activeLevel)
            }
            uiState.nextLevel?.let { nextLevel ->
                item {
                    ComingNextSection(level = nextLevel)
                }
            }
            for (goal in uiState.futureGoals) {
                item {
                    FutureGoalSection(level = goal)
                }
            }
            if (uiState.smallLevels.isNotEmpty()) {
                item {
                    SmallLevelsRow(levels = uiState.smallLevels)
                }
            }
            item {
                StatsSection(
                    streakDays = uiState.streakDays,
                    rank = uiState.rank,
                    rankPosition = uiState.rankPosition,
                    isRankUp = uiState.isRankUp,
                )
            }
        }
    }
}

@Composable
private fun ActiveJourneySection(
    level: LevelData,
    modifier: Modifier = Modifier,
) {
    val progress = if (level.totalUnits > 0) {
        level.completedUnits.toFloat() / level.totalUnits.toFloat()
    } else {
        0f
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "ACTIVE JOURNEY",
            color = LpColors.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Level ${level.number}: ${level.name}",
            color = LpColors.onSurface,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 34.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = LpColors.primary,
            trackColor = LpColors.surfaceContainer,
            strokeCap = StrokeCap.Round,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${level.completedUnits} / ${level.totalUnits} Units Completed",
            color = LpColors.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun UnitsTimeline(
    level: LevelData,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        level.visibleUnits.forEachIndexed { index, unit ->
            val isLast = index == level.visibleUnits.lastIndex && level.hiddenUnitCount == 0
            UnitTimelineItem(
                unit = unit,
                isLast = isLast,
            )
        }
        if (level.hiddenUnitCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${level.hiddenUnitCount} more units in Level ${level.number}...",
                    color = LpColors.onSurfaceVariant,
                    fontSize = 13.sp,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = LpColors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun UnitTimelineItem(
    unit: UnitData,
    isLast: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        // Timeline indicator column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp),
        ) {
            UnitStateIndicator(state = unit.state)
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (unit.state == UnitState.ACTIVE) 100.dp else 60.dp)
                        .background(LpColors.surfaceContainer),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Unit card
        when (unit.state) {
            UnitState.COMPLETED -> CompletedUnitCard(unit = unit, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
            UnitState.ACTIVE -> ActiveUnitCard(unit = unit, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
            UnitState.LOCKED -> LockedUnitCard(unit = unit, modifier = Modifier.weight(1f).padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun UnitStateIndicator(state: UnitState) {
    val (bgColor, iconTint, icon) = when (state) {
        UnitState.COMPLETED -> Triple(LpColors.primary, Color.White, Icons.Default.Check)
        UnitState.ACTIVE -> Triple(LpColors.primary, Color.White, Icons.Default.PlayArrow)
        UnitState.LOCKED -> Triple(LpColors.surfaceContainer, LpColors.onSurfaceVariant, Icons.Default.Lock)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun CompletedUnitCard(
    unit: UnitData,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = LpColors.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UNIT ${unit.number}",
                    color = LpColors.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    text = unit.letters,
                    color = LpColors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "COMPLETED",
                color = LpColors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun ActiveUnitCard(
    unit: UnitData,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(
            width = 2.dp,
            color = LpColors.primary,
            shape = RoundedCornerShape(16.dp),
        ),
        shape = RoundedCornerShape(16.dp),
        color = LpColors.surfaceContainerLowest,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UNIT ${unit.number}",
                    color = LpColors.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                )
                Text(
                    text = unit.letters,
                    color = LpColors.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                unit.progress?.let { progress ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).toInt()}% Progress",
                        color = LpColors.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(LpColors.primary)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "RESUME",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
        }
    }
}

@Composable
private fun LockedUnitCard(
    unit: UnitData,
    modifier: Modifier = Modifier,
) {
    val isCompact = unit.number >= 4

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = LpColors.surfaceContainerLow,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = if (isCompact) 10.dp else 14.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCompact) "Unit ${unit.number}: ${unit.letters}" else unit.letters,
                    color = LpColors.onSurfaceVariant,
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    fontWeight = if (isCompact) FontWeight.Normal else FontWeight.SemiBold,
                )
            }
            Icon(
                imageVector = if (isCompact) Icons.Default.Lock else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = LpColors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ComingNextSection(
    level: LevelData,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "COMING NEXT",
            color = LpColors.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = LpColors.surfaceContainerLow,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = "Level ${level.number}: ${level.name}",
                    color = LpColors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(text = "${level.totalUnits} Units")
                    level.estimatedHours?.let { hours ->
                        InfoChip(text = "Estimated ${hours}h")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(LpColors.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = LpColors.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FutureGoalSection(
    level: LevelData,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "FUTURE GOAL",
            color = LpColors.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = LpColors.surfaceContainerLow,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                Text(
                    text = "Level ${level.number}: ${level.name}",
                    color = LpColors.onSurfaceVariant,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                )
            }
        }
    }
}

@Composable
private fun SmallLevelsRow(
    levels: ImmutableList<LevelData>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for (level in levels) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = LpColors.surfaceContainerLow,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LpColors.surfaceContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = LpColors.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = level.name,
                        color = LpColors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSection(
    streakDays: Int,
    rank: String,
    rankPosition: Int,
    isRankUp: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Streak card
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(LpColors.primary)
                .padding(16.dp),
        ) {
            Column {
                Text(
                    text = "CURRENT STREAK",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$streakDays Days",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        // Rank card
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            color = LpColors.surfaceContainerLowest,
            shadowElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "RANK",
                    color = LpColors.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = rank,
                    color = LpColors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#$rankPosition Overall",
                        color = LpColors.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isRankUp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = LpColors.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningPathBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf(
        BottomNavTab("LEARN", Icons.Default.MenuBook),
        BottomNavTab("PRACTICE", Icons.Default.EditNote),
        BottomNavTab("LEADERBOARD", Icons.Default.BarChart),
        BottomNavTab("PROFILE", Icons.Default.Person),
    )

    NavigationBar(
        modifier = modifier,
        containerColor = LpColors.surfaceContainerLowest,
        tonalElevation = 0.dp,
    ) {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LpColors.primary,
                    selectedTextColor = LpColors.primary,
                    indicatorColor = LpColors.secondaryContainer,
                    unselectedIconColor = LpColors.onSurfaceVariant,
                    unselectedTextColor = LpColors.onSurfaceVariant,
                ),
            )
        }
    }
}

private data class BottomNavTab(val label: String, val icon: ImageVector)

private object LpColors {
    val primary = Color(0xFF0055D9)
    val background = Color(0xFFF8F9FA)
    val surfaceContainerLow = Color(0xFFF1F4F5)
    val surfaceContainerLowest = Color(0xFFFFFFFF)
    val surfaceContainer = Color(0xFFEBEEF0)
    val onSurface = Color(0xFF2D3335)
    val onSurfaceVariant = Color(0xFF5A6062)
    val secondaryContainer = Color(0xFFDBE3F1)
}
