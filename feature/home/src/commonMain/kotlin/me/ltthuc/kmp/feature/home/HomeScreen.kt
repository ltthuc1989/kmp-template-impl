package me.ltthuc.kmp.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.ltthuc.kmp.core.model.LevelCard
import me.ltthuc.kmp.core.model.LevelStatus
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.home_badge_active
import me.ltthuc.kmp.core.resource.home_badge_locked
import me.ltthuc.kmp.core.resource.home_badge_locked_unlock_after
import me.ltthuc.kmp.core.resource.home_badge_ready
import me.ltthuc.kmp.core.resource.home_progress_label
import me.ltthuc.kmp.core.resource.home_start_button
import me.ltthuc.kmp.core.resource.home_title
import me.ltthuc.kmp.core.resource.home_unit_label
import me.ltthuc.kmp.core.resource.level_card_coming_soon
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current

    HomeScreenContent(
        modifier = modifier.fillMaxSize(),
        onSettings = { navBackStack.add(Destination.Setting.Root) },
        content = {
            AsyncLoadContents(
                modifier = Modifier.fillMaxSize(),
                screenState = screenState,
            ) { uiState ->
                LevelList(
                    levels = uiState.levels,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = it,
                    onLevelClick = { levelCard ->
                        navBackStack.add(Destination.Learning.UnitSelection(levelCard.level.id))
                    },
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    onSettings: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        topBar = { HomeTopBar(onSettings = onSettings) },
        bottomBar = { BottomBannerAd() },
    ) { innerPadding ->
        content(innerPadding)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        modifier = Modifier
                            .size(40.dp)
                            .padding(8.dp),
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Text(
                    text = stringResource(Res.string.home_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        actions = {
            IconButton(onClick = onSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun LevelList(
    levels: ImmutableList<LevelCard>,
    contentPadding: PaddingValues,
    onLevelClick: (LevelCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 8.dp,
            start = 16.dp,
            end = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = levels,
            key = { it.level.id },
        ) { card ->
            LevelCardRow(
                card = card,
                onClick = { onLevelClick(card) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LevelCardRow(
    card: LevelCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLocked = card.status is LevelStatus.Locked
    val isComingSoon = card.status is LevelStatus.ComingSoon
    val containerColor = when {
        isLocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isComingSoon -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }
    val isPremiumRequired = (card.status as? LevelStatus.Locked)?.isPremiumRequired == true
    val isInteractive = (!isLocked || isPremiumRequired) && !isComingSoon
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        shadowElevation = if (isLocked || isComingSoon) 0.dp else 2.dp,
        onClick = onClick,
        enabled = isInteractive,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LevelCardThumbnail(status = card.status)
            Box(modifier = Modifier.weight(1f)) {
                when (val status = card.status) {
                    is LevelStatus.Active -> ActiveCardContent(
                        title = card.level.title,
                        unitNumber = status.currentUnit.number,
                        unitTitle = status.currentUnit.title,
                        progressPercent = status.progressPercent,
                    )

                    LevelStatus.ReadyToStart -> ReadyCardContent(title = card.level.title)

                    is LevelStatus.Locked -> LockedCardContent(
                        title = card.level.title,
                        prerequisiteTitle = status.prerequisiteLevel?.title,
                    )

                    LevelStatus.ComingSoon -> ComingSoonCardContent(title = card.level.title)
                }
            }
        }
    }
}

@Composable
private fun LevelCardThumbnail(
    status: LevelStatus,
    modifier: Modifier = Modifier,
) {
    val color = when (status) {
        is LevelStatus.Active -> MaterialTheme.colorScheme.primaryContainer
        LevelStatus.ReadyToStart -> MaterialTheme.colorScheme.tertiaryContainer
        is LevelStatus.Locked -> MaterialTheme.colorScheme.surfaceVariant
        LevelStatus.ComingSoon -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
    }
    val iconTint = when (status) {
        is LevelStatus.Active -> MaterialTheme.colorScheme.onPrimaryContainer
        LevelStatus.ReadyToStart -> MaterialTheme.colorScheme.onTertiaryContainer
        is LevelStatus.Locked -> MaterialTheme.colorScheme.onSurfaceVariant
        LevelStatus.ComingSoon -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    val icon: ImageVector = when (status) {
        is LevelStatus.Locked -> Icons.Outlined.Lock
        LevelStatus.ComingSoon -> Icons.Outlined.Schedule
        else -> Icons.AutoMirrored.Outlined.MenuBook
    }

    Box(
        modifier = modifier
            .size(width = 64.dp, height = 80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
        )
    }
}

@Composable
private fun ActiveCardContent(
    title: String,
    unitNumber: Int,
    unitTitle: String,
    progressPercent: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiary,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    text = stringResource(Res.string.home_badge_active),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.home_unit_label, unitNumber, unitTitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            progress = { progressPercent / PERCENT_DIVISOR },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.home_progress_label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ReadyCardContent(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.home_badge_ready),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Button(
            onClick = { /* TODO: navigate to unit selection when implemented */ },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(
                text = stringResource(Res.string.home_start_button),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LockedCardContent(
    title: String,
    prerequisiteTitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (prerequisiteTitle != null) {
                stringResource(Res.string.home_badge_locked_unlock_after, prerequisiteTitle)
            } else {
                stringResource(Res.string.home_badge_locked)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ComingSoonCardContent(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                text = stringResource(Res.string.level_card_coming_soon),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
    }
}

private const val PERCENT_DIVISOR = 100f
