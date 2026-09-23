package me.ltthuc.kmp.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ltthuc.kmp.core.content.ContentBytes
import me.ltthuc.kmp.core.model.Level
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
import me.ltthuc.kmp.core.ui.dialog.ParentalGateScreen
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.screen.view.LocalFloatingNavHeight
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.core.ui.utils.fadeOutBottom
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val navBackStack = LocalNavBackStack.current
    val lang = LocalAppLanguage.current
    // Settings sits behind a parental gate; show it in-place so the Home backstack is kept.
    var showSettingsGate by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        HomeScreenContent(
            modifier = Modifier.fillMaxSize(),
            onSettings = { showSettingsGate = true },
            content = { innerPadding ->
                // The pill nav floats over this list. Reserve its height at the bottom so the last
                // card can still be scrolled clear of it, and dissolve whatever slides underneath
                // rather than letting cards run into the pill.
                val navHeight = LocalFloatingNavHeight.current
                AsyncLoadContents(
                    modifier = Modifier.fillMaxSize(),
                    screenState = screenState,
                ) { uiState ->
                    LevelList(
                        levels = uiState.levels,
                        modifier = Modifier.fillMaxSize().fadeOutBottom(navHeight),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding() + navHeight,
                        ),
                        onLevelClick = { levelCard ->
                            navBackStack.add(Destination.Learning.UnitSelection(levelCard.level.id))
                        },
                    )
                }
            },
        )

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
            Text(
                text = stringResource(Res.string.home_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
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
        BoxWithConstraints {
            // Máy nhỏ (thẻ hẹp hơn 360dp, tức màn dưới ~392dp): hình, khe, lề thẻ và nút Start cùng
            // thu lại để nhường chỗ cho chữ — "Letter Combinations" cạnh nút Start vẫn nằm trên
            // một dòng ở cỡ chữ thường. Hẹp hơn nữa thì [OneLineCardText] tự thu cỡ chữ.
            val compact = maxWidth < COMPACT_CARD_WIDTH
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (compact) 12.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                LevelCardThumbnail(
                    level = card.level,
                    status = card.status,
                    artSize = if (compact) COMPACT_ART_SIZE else ART_SIZE,
                )
                Box(modifier = Modifier.weight(1f)) {
                    when (val status = card.status) {
                        is LevelStatus.Active -> ActiveCardContent(
                            title = card.level.title,
                            unitNumber = status.currentUnit.number,
                            unitTitle = status.currentUnit.title,
                            progressPercent = status.progressPercent,
                        )

                        LevelStatus.ReadyToStart -> ReadyCardContent(
                            title = card.level.title,
                            onStart = onClick,
                            compact = compact,
                        )

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
}

/**
 * Chữ trên thẻ level luôn nằm MỘT dòng: thiếu chỗ thì chữ nhỏ dần (tới [CARD_TEXT_MIN_SIZE]), chỉ
 * khi nhỏ hết cỡ vẫn không vừa mới cắt "…". Tiêu đề gãy đôi ("Letter / Combinations") làm thẻ cao
 * lệch các thẻ khác và trông như lỗi (user báo 2026-09-19, máy màn nhỏ).
 */
@Composable
private fun OneLineCardText(
    text: String,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val maxSize = style.fontSize.takeIf { it.isSp } ?: CARD_TEXT_FALLBACK_SIZE
    Text(
        modifier = modifier,
        text = text,
        style = style,
        fontWeight = fontWeight,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(
            minFontSize = if (maxSize.value < CARD_TEXT_MIN_SIZE.value) maxSize else CARD_TEXT_MIN_SIZE,
            maxFontSize = maxSize,
            stepSize = 0.5.sp,
        ),
    )
}

@Composable
private fun LevelCardThumbnail(
    level: Level,
    status: LevelStatus,
    artSize: Dp,
    modifier: Modifier = Modifier,
) {
    val iconTint = when (status) {
        is LevelStatus.Active -> MaterialTheme.colorScheme.onPrimaryContainer
        LevelStatus.ReadyToStart -> MaterialTheme.colorScheme.onTertiaryContainer
        is LevelStatus.Locked -> MaterialTheme.colorScheme.onSurfaceVariant
        LevelStatus.ComingSoon -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    // Khoá / sắp ra mắt giữ nguyên biểu tượng cũ: ổ khoá và đồng hồ là TRẠNG THÁI, còn hình
    // của level nói level dạy gì — trộn hai thứ vào một ô thì mất nghĩa của cả hai.
    val stateIcon: ImageVector? = when (status) {
        is LevelStatus.Locked -> Icons.Outlined.Lock
        LevelStatus.ComingSoon -> Icons.Outlined.Schedule
        else -> null
    }
    val art = levelArt(level.number)

    // Không ô nền phía sau (chốt 2026-09-18): nhân vật đã có nét viền và màu riêng, đặt thêm một ô
    // màu sau lưng là hai lớp nền chồng nhau trên cùng một thẻ. Bỏ ô đi thì hình được vẽ to hơn
    // trong cùng khoảng chỗ cũ.
    Box(
        modifier = modifier.size(width = artSize, height = artSize + 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            stateIcon != null -> Icon(
                modifier = Modifier.size(32.dp),
                imageVector = stateIcon,
                contentDescription = null,
                tint = iconTint,
            )

            art != null -> Image(
                bitmap = art,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(artSize),
            )

            // Ảnh chưa nạp xong hoặc thiếu file: vẫn phải có gì đó trong ô, nếu không thẻ
            // trông như đang hỏng. Quyển sách cũ làm đúng việc đó.
            else -> Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                contentDescription = null,
                tint = iconTint,
            )
        }
    }
}

/**
 * Hình của level, nằm sẵn trong app tại `files/images/levels/level_<n>.webp` — cùng nét vẽ với
 * ảnh từ vựng (sinh bằng `opw_audio_project/scripts/generate_level_icons.py`).
 *
 * Thiếu file thì trả null và GHI LOG: thẻ rơi về quyển sách, nhìn vẫn bình thường, nên không log
 * là không ai biết ảnh đã rụng.
 */
@Composable
private fun levelArt(number: Int): ImageBitmap? {
    val contentBytes: ContentBytes = koinInject()
    return produceState<ImageBitmap?>(initialValue = null, number) {
        val path = "files/images/levels/level_$number.webp"
        value = withContext(Dispatchers.Default) {
            runCatching {
                val bytes = contentBytes.load(path) ?: error("no bytes")
                bytes.decodeToImageBitmap()
            }
                .onFailure { Napier.w(tag = "HomeScreen") { "No level art at $path — falling back to the book icon" } }
                .getOrNull()
        }
    }.value
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
            OneLineCardText(
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

        // Dòng nội dung của thẻ in ĐẬM: ở cỡ bodyMedium nét thường, "Unit 5: th th ck qu" chìm
        // xuống dưới tiêu đề level và mắt không bắt được nó (user báo 2026-09-18).
        OneLineCardText(
            text = stringResource(Res.string.home_unit_label, unitNumber, unitTitle),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
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
    onStart: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            OneLineCardText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OneLineCardText(
                text = stringResource(Res.string.home_badge_ready),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
        // Cùng đích với cú chạm vào cả thẻ. Nút nằm ĐÈ lên thẻ nên nó nuốt cú chạm: để
        // onClick rỗng thì bấm trúng chữ "Start" là không có gì xảy ra, dù bấm chỗ khác
        // trên thẻ vẫn vào được — nhìn như app đơ.
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = if (compact) COMPACT_START_PADDING else ButtonDefaults.ContentPadding,
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
        OneLineCardText(
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
        OneLineCardText(
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

private val COMPACT_CARD_WIDTH = 360.dp
private val ART_SIZE = 64.dp
private val COMPACT_ART_SIZE = 52.dp
private val COMPACT_START_PADDING = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
private val CARD_TEXT_MIN_SIZE = 12.sp
private val CARD_TEXT_FALLBACK_SIZE = 16.sp
