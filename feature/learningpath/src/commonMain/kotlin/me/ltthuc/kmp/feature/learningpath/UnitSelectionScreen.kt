package me.ltthuc.kmp.feature.learningpath

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.model.FREE_UNITS_PER_LEVEL
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED
import me.ltthuc.kmp.core.model.UnitCard
import me.ltthuc.kmp.core.model.UnitLetterPreview
import me.ltthuc.kmp.core.model.UnitStatus
import me.ltthuc.kmp.core.repository.ContentPackRepository
import me.ltthuc.kmp.core.repository.LevelAccess
import me.ltthuc.kmp.core.repository.PackState
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.level_name
import me.ltthuc.kmp.core.resource.unit_free_label
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.core.ui.components.PuffySurface
import me.ltthuc.kmp.core.ui.dialog.ParentalGateScreen
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.ScreenState
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val AccentRed = Color(0xFFE63946)
private val WarnAmber = Color(0xFF9A6B00)
private val SoftPink = Color(0xFFF7B4BC)
private val LockedGray = Color(0xFFE4E7EB)
private val LockedTextGray = Color(0xFF9AA3AF)

/**
 * Until a pack has been looked at, assume it is [UnitContent.Ready]: showing a download
 * badge that resolves away a frame later would flicker on every unit that ships in the app.
 */
private fun PackState?.toUnitContent(): UnitContent = when (this) {
    null, PackState.Bundled, PackState.Ready -> UnitContent.Ready
    is PackState.NotDownloaded -> UnitContent.NeedsDownload
    is PackState.Downloading -> UnitContent.Downloading(progress.fraction)
    is PackState.Failed -> UnitContent.Failed(retryable = retryable)
}

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
    val packRepository: ContentPackRepository = koinInject()
    val levelAccess: LevelAccess = koinInject()
    val unitScope = rememberCoroutineScope()
    val packStates by packRepository.packStates.collectAsStateWithLifecycle()
    val lang = LocalAppLanguage.current
    // Settings sits behind a parental gate; show it in-place so the backstack (→ unit list) is kept.
    var showSettingsGate by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            // Match the Lesson Map background (surface), so the two list screens look identical.
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                val uiState = (screenState as? ScreenState.Idle)?.data
                UnitTopBar(
                    title = uiState
                        ?.let { stringResource(Res.string.level_name, it.level.number, it.level.title) }
                        .orEmpty(),
                    // Back always lands on Home. Usually that is a plain pop, but a cold start can
                    // restore straight into this screen (lastScreen = UNIT_LIST) with nothing to
                    // pop — and there is no bottom nav here, so hiding the button (what we used to
                    // do) left the child with no way out of the level.
                    onBack = {
                        if (navBackStack.size > 1) {
                            navBackStack.removeAt(navBackStack.size - 1)
                        } else {
                            navBackStack.clear()
                            navBackStack.add(Destination.Home)
                        }
                    },
                    onSettings = { showSettingsGate = true },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = { BottomBannerAd() },
        ) { innerPadding ->
            AsyncLoadContents(
                modifier = Modifier.fillMaxSize(),
                screenState = screenState,
                // Transparent so the Scaffold surface background shows through instead of being
                // painted over by AsyncLoadContents' default background (matches LessonMapScreen).
                containerColor = Color.Transparent,
            ) { uiState ->
                // Ask the store which units are already on the device. Cheap (a manifest map
                // lookup plus a file check per pack) and it settles before the first frame the
                // child could tap, so no badge appears and then vanishes.
                LaunchedEffect(uiState.units) {
                    uiState.units.forEach { packRepository.refresh(it.unit.id) }
                }

                // Opening a level the parent has paid for is the signal to fetch it. One trigger,
                // here: buying ends by returning to this very screen, so a second hook on the
                // purchase itself would be the same work down a second path. Gated on access so
                // browsing a level nobody bought never pulls 8.7MB.
                LaunchedEffect(levelId) {
                    if (levelAccess.canOpenPaidUnitsNow(levelId)) {
                        packRepository.downloadLevelInBackground(levelId)
                    }
                }

                UnitSelectionList(
                    units = uiState.units,
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 12.dp,
                        bottom = innerPadding.calculateBottomPadding() + 12.dp,
                        start = 16.dp,
                        end = 16.dp,
                    ),
                    contentFor = { card -> packStates[card.unit.id].toUnitContent() },
                    onUnitClick = { card ->
                        when (card.status) {
                            // Paid unit not owned → open the features/paywall screen directly (kid-safe
                            // marketing). The parental gate appears on the purchase button there. No
                            // content is fetched: nothing has been bought yet.
                            UnitStatus.PremiumLocked -> navBackStack.add(
                                Destination.Paywall(
                                    source = Destination.Paywall.Source.UNIT_LOCKED,
                                    levelId = levelId,
                                    gatedAlready = false,
                                ),
                            )
                            // Everything else: make sure the content is on the device, then apply
                            // the ordinary rule. Getting the data first means a unit whose earlier
                            // download failed retries on the very tap that wanted it, and the
                            // sequential gate below stays the single place that decides entry.
                            else -> unitScope.launch {
                                val packId = card.unit.id
                                if (!packRepository.ensureReady(packId)) return@launch

                                if (card.status == UnitStatus.Locked) {
                                    sfx.playPrompt("vp_locked", lang)
                                } else {
                                    navBackStack.add(Destination.Learning.LessonMap(levelId, packId))
                                }
                            }
                        }
                    },
                )
            }
        }

        // Parental gate before Settings. Shown in the parent's language (kid screens are forced EN).
        if (showSettingsGate) {
            CompositionLocalProvider(LocalAppLocale provides lang) {
                ParentalGateScreen(
                    modifier = Modifier.fillMaxSize(),
                    onPass = {
                        showSettingsGate = false
                        navBackStack.add(Destination.Setting.Root)
                    },
                    onDismiss = { showSettingsGate = false },
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
    contentFor: (UnitCard) -> UnitContent,
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
                content = contentFor(card),
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
    content: UnitContent,
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
            content = content,
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
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
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
        // Opaque surface (same as the screen background) so it hides content scrolling under the
        // status bar — but invisible since it matches the bg. Both states use the same opaque color,
        // so the cross-fade never lerps through transparent-black → no black flash on scroll.
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
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
    content: UnitContent,
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
                ActionPlayButton(status = card.status, content = content)
            }
            if (!isLocked) {
                card.unit.themeChip?.let { theme ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            // Inset past the action button so the chip never sits on the
                            // check/play circle.
                            .padding(top = 6.dp, end = ChipEndInset),
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

/**
 * Whether this unit's audio and pictures are on the device — orthogonal to [UnitStatus],
 * which says whether the child is allowed in. A unit can be unlocked but not yet fetched.
 */
internal sealed interface UnitContent {
    /** In the app or already downloaded. */
    data object Ready : UnitContent
    data object NeedsDownload : UnitContent
    data class Downloading(val fraction: Float) : UnitContent

    /** [retryable] false = out of space or a missing file; tapping again cannot help. */
    data class Failed(val retryable: Boolean) : UnitContent
}

/**
 * The card's trailing slot. It already carried "may I enter?" (play / lock); it now also
 * carries "is it here?" — one slot, one glance, instead of a second indicator elsewhere.
 * Lock always wins: an unpurchased unit must never show a download affordance.
 */
@Composable
private fun ActionPlayButton(status: UnitStatus, content: UnitContent) {
    // Content comes first for every state except an unpurchased unit: play must never appear
    // before the audio is actually on the device, or a child taps into a silent lesson. Locked
    // units draw the same states in the locked greys — they are status, not something to tap.
    if (status != UnitStatus.PremiumLocked) {
        val locked = status == UnitStatus.Locked
        val accent = if (locked) LockedTextGray else AccentRed

        when (content) {
            is UnitContent.NeedsDownload -> {
                SlotCircle(tint = accent) {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "Download this unit",
                        tint = accent,
                        modifier = Modifier.size(17.dp),
                    )
                }
                return
            }
            is UnitContent.Downloading -> {
                val animated by animateFloatAsState(content.fraction, label = "unitDownload")
                Box(modifier = Modifier.size(PlayButtonSize), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animated },
                        modifier = Modifier.fillMaxSize(),
                        color = accent,
                        trackColor = if (locked) LockedGray else AccentRed.copy(alpha = 0.16f),
                        strokeWidth = 2.5.dp,
                        strokeCap = StrokeCap.Round,
                    )
                    // The arrow stays put while the ring fills around it: the slot keeps saying
                    // "this is being fetched" rather than swapping to a number too small to read
                    // at 26dp.
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = "Downloading",
                        tint = accent,
                        modifier = Modifier.size(13.dp),
                    )
                }
                return
            }
            is UnitContent.Failed -> {
                // Retry lives on the unit that failed, so the tap that wants the lesson is the
                // tap that fetches it again. The downloader has already tried 3 times per file.
                val tint = if (content.retryable) accent else WarnAmber
                SlotCircle(tint = tint) {
                    Icon(
                        imageVector = if (content.retryable) Icons.Filled.Refresh else Icons.Filled.Warning,
                        contentDescription = if (content.retryable) "Retry download" else "Cannot download",
                        tint = tint,
                        modifier = Modifier.size(17.dp),
                    )
                }
                return
            }
            UnitContent.Ready -> Unit
        }
    }

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
        // Content is here, the previous lesson is not finished. Greyed play rather than an
        // empty slot: the unit is complete and waiting, which is different from having nothing.
        UnitStatus.Locked -> Box(
            modifier = Modifier
                .size(PlayButtonSize)
                .clip(CircleShape)
                .border(1.5.dp, LockedTextGray, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Finish the previous lesson first",
                tint = LockedTextGray,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SlotCircle(tint: Color = AccentRed, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(PlayButtonSize)
            .clip(CircleShape)
            .border(1.5.dp, tint, CircleShape),
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

@Composable
private fun ThemeChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(18.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            // Tight line height so the pill hugs the glyphs instead of the default 1.4× box.
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

// Card end padding (14) + play/check button (34) + a small gap, so the theme chip lands to the
// left of the action button instead of over it.
private val ChipEndInset = 14.dp + PlayButtonSize + 6.dp
