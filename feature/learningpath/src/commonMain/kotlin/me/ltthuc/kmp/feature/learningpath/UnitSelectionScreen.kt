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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.model.FREE_UNITS_PER_LEVEL
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED
import me.ltthuc.kmp.core.model.UnitCard
import me.ltthuc.kmp.core.model.UnitLetterPreview
import me.ltthuc.kmp.core.model.UnitStatus
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.unit_free_label
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

private val ScreenBg = Color(0xFFFCE4E7)
private val AccentRed = Color(0xFFE63946)
private val SoftPink = Color(0xFFF7B4BC)
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
    val sfx: SfxController = koinInject()
    val lang = LocalAppLanguage.current

    val isStartDestination = navBackStack.size == 1
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = ScreenBg,
            topBar = {
                val uiState = (screenState as? ScreenState.Idle)?.data
                UnitTopBar(
                    title = uiState?.let { "Book ${it.level.number}: ${it.level.title}" }.orEmpty(),
                    showBackButton = !isStartDestination,
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
                            // Sequential lock (level already paid, but an earlier unit isn't finished —
                            // or a not-yet-reached free unit) → speak the "finish the previous one" guide.
                            UnitStatus.Locked -> sfx.playPrompt("vp_locked", lang)
                            // Paid unit not owned → open the features/paywall screen directly (kid-safe
                            // marketing). The parental gate appears on the purchase button there.
                            UnitStatus.PremiumLocked -> navBackStack.add(
                                Destination.Paywall(
                                    source = Destination.Paywall.Source.UNIT_LOCKED,
                                    levelId = levelId,
                                    gatedAlready = false,
                                ),
                            )
                            // Any unlocked/active/completed unit opens the full-screen Lesson Map,
                            // which handles per-lesson lock state and resume/replay.
                            UnitStatus.Completed,
                            UnitStatus.Active,
                            UnitStatus.Unlocked,
                            -> navBackStack.add(Destination.Learning.LessonMap(levelId, card.unit.id))
                        }
                    },
                )
            }
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
    showBackButton: Boolean,
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
private fun UnitNumberBadge(status: UnitStatus, unitNumber: Int) {
    val isFreeZone = MONETIZATION_ENABLED && unitNumber <= FREE_UNITS_PER_LEVEL
    val isLocked = status == UnitStatus.Locked || status == UnitStatus.PremiumLocked
    val onRed = status == UnitStatus.Active ||
        status == UnitStatus.Unlocked ||
        status == UnitStatus.Completed

    val container = when {
        onRed -> AccentRed
        isFreeZone && isLocked -> SoftPink
        isLocked -> LockedGray
        else -> Color.White
    }

    // Outer box (not clipped) so the small lock badge can overhang the circle's corner.
    Box(contentAlignment = Alignment.Center) {
        // Ripple pulse on the active unit's circle. Smaller maxScale so the rings stay within the
        // timeline column and don't bleed into the card to the right.
        PulseRings(
            isActive = status == UnitStatus.Active || status == UnitStatus.Unlocked,
            ringColor = AccentRed,
            maxScale = TIMELINE_PULSE_MAX_SCALE,
        )
        PuffySurface(
            shape = CircleShape,
            containerColor = container,
            shadowElevation = 8.dp,
            shadowTint = if (onRed || (isFreeZone && isLocked)) AccentRed else LockedTextGray,
            shadowAlpha = 0.40f,
            topHighlightHeight = 14.dp,
            topHighlightAlpha = 0.55f,
            bottomShadeHeight = 14.dp,
            bottomShadeAlpha = 0.16f,
            modifier = Modifier.size(UnitBadgeSize),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    status == UnitStatus.Completed -> Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    isFreeZone -> Text(
                        text = stringResource(Res.string.unit_free_label),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onRed) Color.White else Color.White.copy(alpha = 0.95f),
                    )
                    isLocked -> Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = LockedTextGray,
                        modifier = Modifier.size(18.dp),
                    )
                    else -> Text(
                        text = unitNumber.toString().padStart(2, '0'),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (onRed) Color.White else AccentRed,
                    )
                }
            }
        }
        // Monkey-style small lock badge on a free-but-still-sequential-locked node.
        if (isFreeZone && isLocked) {
            BadgeLock(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp))
        }
    }
}

@Composable
private fun BadgeLock(modifier: Modifier = Modifier) {
    PuffySurface(
        shape = CircleShape,
        containerColor = Color.White,
        shadowElevation = 4.dp,
        shadowTint = LockedTextGray,
        shadowAlpha = 0.35f,
        topHighlightHeight = 6.dp,
        topHighlightAlpha = 0.7f,
        bottomShadeHeight = 6.dp,
        bottomShadeAlpha = 0.06f,
        modifier = modifier.size(20.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = LockedTextGray,
                modifier = Modifier.size(11.dp),
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
    // Sequential lock = greyed + not clickable; premium lock = greyed but tappable (opens paywall).
    val isSequentialLocked = card.status == UnitStatus.Locked
    val isPremiumLocked = card.status == UnitStatus.PremiumLocked
    val isLocked = isSequentialLocked || isPremiumLocked
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
        // Always tappable: a sequential-locked tap plays the spoken guide instead of navigating.
        // PremiumLocked taps still open the paywall. Dim visuals are driven by isLocked below.
        enabled = true,
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
            defaultElevation = if (isLocked) 5.dp else 10.dp,
            disabledElevation = 5.dp,
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
            if (!isLocked) {
                card.unit.themeChip?.let { theme ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 12.dp),
                    ) {
                        ThemeChip(label = theme)
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
        UnitStatus.PremiumLocked -> Box(
            modifier = Modifier
                .size(PlayButtonSize)
                .clip(CircleShape)
                .background(LockedGray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked – purchase to unlock",
                tint = LockedTextGray,
                modifier = Modifier.size(16.dp),
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

// Keep the pulse rings inside the 56dp timeline column (48dp badge → ~65dp) so they don't
// overlap the unit card 12dp to the right.
private const val TIMELINE_PULSE_MAX_SCALE = 1.35f
private val UnitBadgeSize = 48.dp
private val PlayButtonSize = 34.dp
private val TimelineColumnWidth = 56.dp
private val ConnectorStrokeWidth = 2.dp
private val CardVerticalPadding = 6.dp
private val CardMinHeight = 76.dp
private val CardCornerRadius = 20.dp
