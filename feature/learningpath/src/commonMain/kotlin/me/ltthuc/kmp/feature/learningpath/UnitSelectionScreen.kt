package me.ltthuc.kmp.feature.learningpath

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.UnitCard
import me.ltthuc.kmp.core.model.UnitStatus
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val ScreenBg = Color(0xFFFCE4E7)
private val AccentRed = Color(0xFFE63946)
private val SoftPink = Color(0xFFF7B4BC)
private val CompletedGreen = Color(0xFF2FBF71)
private val LockedGray = Color(0xFFE4E7EB)
private val LockedTextGray = Color(0xFF9AA3AF)

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
    val scope = rememberCoroutineScope()

    val isStartDestination = navBackStack.size == 1
    Scaffold(
        modifier = modifier,
        containerColor = ScreenBg,
        topBar = {
            val uiState = (screenState as? ScreenState.Idle)?.data
            UnitTopBar(
                title = uiState?.let { "Book ${it.level.number}: ${it.level.title}" }.orEmpty(),
                startedCount = uiState?.startedCount ?: 0,
                totalUnits = uiState?.units?.size ?: 0,
                showBackButton = !isStartDestination,
                onBack = { if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.size - 1) },
                onSettings = { navBackStack.add(Destination.Setting.Root) },
            )
        },
    ) { innerPadding ->
        AsyncLoadContents(
            modifier = Modifier.fillMaxSize(),
            screenState = screenState,
        ) { uiState ->
            UnitSelectionList(
                units = uiState.units,
                contentPadding = innerPadding,
                onUnitClick = { card ->
                    if (card.status != UnitStatus.Locked) {
                        scope.launch {
                            // Resume from saved position if the user last left this unit, else
                            // start fresh. Clamp to a visible step in case the saved position
                            // landed on a step that is hidden for this level.
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
                },
            )
        }
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
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 12.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(TimelineRowSpacing),
    ) {
        items(
            items = units,
            key = { it.unit.id },
        ) { card ->
            UnitRow(
                card = card,
                isLast = card.unit.orderIndex == units.lastIndex,
                onClick = { onUnitClick(card) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitTopBar(
    title: String,
    startedCount: Int,
    totalUnits: Int,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
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
private fun UnitRow(
    card: UnitCard,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        TimelineColumn(
            status = card.status,
            unitNumber = card.unit.number,
            isLast = isLast,
        )
        Spacer(Modifier.width(16.dp))
        UnitCardItem(
            card = card,
            onClick = onClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TimelineColumn(
    status: UnitStatus,
    unitNumber: Int,
    isLast: Boolean,
) {
    Column(
        modifier = Modifier.width(TimelineColumnWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TimelineNode(status = status, unitNumber = unitNumber)
        if (!isLast) {
            VerticalConnector(
                modifier = Modifier
                    .height(ConnectorHeight)
                    .width(ConnectorStrokeWidth),
            )
        }
    }
}

@Composable
private fun TimelineNode(status: UnitStatus, unitNumber: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(NodeSize)) {
        if (status == UnitStatus.Active) {
            PulsingRing(
                color = AccentRed,
                modifier = Modifier.size(NodeSize),
            )
        }
        NodeContent(status = status, unitNumber = unitNumber)
    }
}

@Composable
private fun NodeContent(status: UnitStatus, unitNumber: Int) {
    val elevation = if (status == UnitStatus.Locked) 0.dp else NodeElevation
    val shadowColor = when (status) {
        UnitStatus.Completed -> AccentRed
        UnitStatus.Active -> AccentRed
        UnitStatus.Unlocked -> SoftPink
        UnitStatus.Locked -> Color.Transparent
    }
    val baseModifier = Modifier
        .size(NodeSize)
        .shadow(
            elevation = elevation,
            shape = CircleShape,
            ambientColor = shadowColor.copy(alpha = 0.25f),
            spotColor = shadowColor.copy(alpha = 0.45f),
        )
        .clip(CircleShape)
    when (status) {
        UnitStatus.Completed -> Box(
            modifier = baseModifier.background(AccentRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
        UnitStatus.Active -> Box(
            modifier = baseModifier
                .background(Color.White)
                .border(width = 3.dp, color = AccentRed, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            UnitLabel(unitNumber = unitNumber, color = AccentRed)
        }
        UnitStatus.Unlocked -> Box(
            modifier = baseModifier
                .background(Color.White)
                .border(width = 2.dp, color = SoftPink, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            UnitLabel(unitNumber = unitNumber, color = AccentRed)
        }
        UnitStatus.Locked -> Box(
            modifier = baseModifier.background(LockedGray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = LockedTextGray,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
private fun UnitLabel(unitNumber: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Unit",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
        )
        Text(
            text = "$unitNumber",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun PulsingRing(color: Color, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scale",
    )
    val alpha by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "alpha",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun VerticalConnector(modifier: Modifier = Modifier) {
    val connectorColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier.drawBehind {
            val strokeWidth = ConnectorStrokeWidth.toPx()
            val centerX = size.width / 2f
            drawLine(
                color = connectorColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, size.height),
                strokeWidth = strokeWidth,
            )
        },
    )
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

    ElevatedCard(
        onClick = onClick,
        enabled = !isLocked,
        modifier = modifier.defaultMinSize(minHeight = CardMinHeight),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = card.unit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocked) LockedTextGray else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    card.unit.themeChip?.takeIf { !isLocked }?.let { theme ->
                        Spacer(Modifier.width(8.dp))
                        ThemeChip(label = theme)
                    }
                }
                if (!isLocked && card.previewEmojis.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = card.previewEmojis.take(MAX_PREVIEW_EMOJIS).joinToString(" "),
                        fontSize = 20.sp,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            ActionIcon(status = card.status)
        }
    }
}

@Composable
private fun ActionIcon(status: UnitStatus) {
    when (status) {
        UnitStatus.Completed -> Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Completed",
            tint = CompletedGreen,
            modifier = Modifier.size(24.dp),
        )
        UnitStatus.Active, UnitStatus.Unlocked -> Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play",
            tint = AccentRed,
            modifier = Modifier.size(24.dp),
        )
        UnitStatus.Locked -> Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun ThemeChip(label: String) {
    androidx.compose.material3.SuggestionChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        modifier = Modifier.height(24.dp),
    )
}

private const val MAX_PREVIEW_EMOJIS = 4
private val NodeSize = 64.dp
private val NodeElevation = 8.dp
private val TimelineColumnWidth = 80.dp
private val TimelineRowSpacing = 0.dp
private val ConnectorHeight = 36.dp
private val ConnectorStrokeWidth = 4.dp
private val CardMinHeight = 76.dp
private val CardCornerRadius = 20.dp
