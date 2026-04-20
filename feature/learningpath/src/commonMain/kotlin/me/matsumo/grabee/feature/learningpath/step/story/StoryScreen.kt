package me.matsumo.grabee.feature.learningpath.step.story

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.matsumo.grabee.core.model.VocabularyItem
import me.matsumo.grabee.core.model.Word
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
import me.matsumo.grabee.feature.learningpath.step.common.KaraokeText
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PulseRings
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import me.matsumo.grabee.feature.learningpath.step.common.StepNavRow
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun StoryScreen(
    unitId: String,
    wordIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StoryViewModel = koinViewModel { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val safeIndex = wordIndex.coerceIn(0, uiState.words.lastIndex)
        StoryContent(
            currentWord = uiState.words[safeIndex],
            words = uiState.words,
            currentIndex = safeIndex,
            totalWords = uiState.words.size,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
        )
    }
}

@Composable
private fun StoryContent(
    currentWord: Word,
    words: ImmutableList<Word>,
    currentIndex: Int,
    totalWords: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val letterChar = currentWord.text.firstOrNull() ?: '?'
    val letterUpper = letterChar.uppercaseChar().toString()
    val letterLower = letterChar.lowercaseChar().toString()

    // Data-driven: 1 story page per vocabulary item. Fallback: single page using the word itself
    // so letters without vocabulary still render a story.
    val pages: List<VocabularyItem> = remember(currentWord.id) {
        currentWord.vocabulary.ifEmpty { listOf(currentWord.toVocabItem()) }
    }
    val pageCount = pages.size

    val pagerState = rememberPagerState(pageCount = { pageCount })
    val currentPage by remember { derivedStateOf { pagerState.currentPage.coerceIn(0, pages.lastIndex) } }
    val scope = rememberCoroutineScope()

    // Reset pager to page 0 when letter changes (word swipe forward via StepNavRow Next).
    LaunchedEffect(currentWord.id) {
        pagerState.scrollToPage(0)
    }

    var isNarrating by remember(currentWord.id, currentPage) { mutableStateOf(false) }
    LaunchedEffect(currentWord.id, currentPage, isNarrating) {
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
                currentIndex = currentIndex,
                totalWords = totalWords,
                onClose = onClose,
            )
        },
        bottomBar = {
            LetterStepperBar(
                words = words,
                currentIndex = currentIndex,
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
                    pages[currentPage].text,
                ),
                isPlaying = isNarrating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(24.dp))
            AudioPlayButton(
                isNarrating = isNarrating,
                onToggle = { isNarrating = !isNarrating },
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
    pages: List<VocabularyItem>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val frameColor = MaterialTheme.colorScheme.primaryContainer
    val innerImageBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    val canPrev = pagerState.currentPage > 0
    val canNext = pagerState.currentPage < pages.lastIndex

    Box(modifier = Modifier.fillMaxWidth()) {
        // 3-layer comic-frame structure matching code.html:
        //   rounded-xl outer frame + 8dp alpha colored border (comic frame feel) +
        //   p-4 white inner padding + rounded-lg inner image area with soft tint background.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(STORY_CARD_ASPECT)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(STORY_OUTER_CORNER_DP.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                )
                .clip(RoundedCornerShape(STORY_OUTER_CORNER_DP.dp))
                .background(Color.White)
                .border(
                    width = STORY_BORDER_WIDTH_DP.dp,
                    color = frameColor.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(STORY_OUTER_CORNER_DP.dp),
                )
                .padding(STORY_FRAME_PADDING_DP.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(STORY_INNER_CORNER_DP.dp))
                    .background(innerImageBg),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    StoryPageCard(item = pages[pageIndex])
                }
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
private fun StoryPageCard(item: VocabularyItem) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = item.emoji.orEmpty().ifEmpty { "📖" },
            fontSize = 120.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageDotsRow(currentPage: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            val isActive = index == currentPage
            val dotColor = if (isActive) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val elevation = if (isActive) 2.dp else 0.dp
            Box(
                modifier = Modifier
                    .size(DOT_SIZE_DP.dp)
                    .shadow(elevation = elevation, shape = CircleShape)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
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

@Composable
private fun AudioPlayButton(isNarrating: Boolean, onToggle: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier.size(AUDIO_RING_SIZE_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        PulseRings(
            isActive = isNarrating,
            ringColor = primary.copy(alpha = 0.35f),
        )
        IconButton(
            onClick = onToggle,
            modifier = Modifier
                .size(AUDIO_BUTTON_SIZE_DP.dp)
                .shadow(elevation = 10.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(primary, primaryContainer),
                    ),
                ),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(Res.string.story_audio_cd),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

private fun Word.toVocabItem(): VocabularyItem = VocabularyItem(
    text = text,
    emoji = emoji,
    imageAsset = imageAsset,
    audioAsset = wordAudioAsset,
    orderIndex = 0,
)

private const val NARRATION_STUB_MS = 2_400L
private const val STORY_CARD_ASPECT = 4f / 3f
private const val STORY_OUTER_CORNER_DP = 32
private const val STORY_BORDER_WIDTH_DP = 8
private const val STORY_FRAME_PADDING_DP = 16
private const val STORY_INNER_CORNER_DP = 16
private const val DOT_SIZE_DP = 10
private const val CHEVRON_SIZE_DP = 48
private const val AUDIO_RING_SIZE_DP = 72
private const val AUDIO_BUTTON_SIZE_DP = 64
