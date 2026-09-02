package me.ltthuc.kmp.feature.learningpath.step.story

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ltthuc.kmp.core.audio.AudioRef
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.content.ContentBytes
import me.ltthuc.kmp.core.model.Story
import me.ltthuc.kmp.core.model.StoryScene
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LessonProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.slide_next_cd
import me.ltthuc.kmp.core.resource.slide_previous_cd
import me.ltthuc.kmp.core.resource.step_guide_story
import me.ltthuc.kmp.core.resource.story_audio_cd
import me.ltthuc.kmp.core.ui.audio.rememberAudioSession
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.core.ui.theme.LocalPhonicsFontFamily
import me.ltthuc.kmp.feature.learningpath.STORY_PROGRESS_ID
import me.ltthuc.kmp.feature.learningpath.step.DEFAULT_VISIBLE_STEPS
import me.ltthuc.kmp.feature.learningpath.step.STORY_SEGMENT_INDEX
import me.ltthuc.kmp.feature.learningpath.step.common.KaraokeText
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.StepChevronButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepContinueButton
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val UNIT_STORY_STEP_INDEX = STORY_SEGMENT_INDEX // = 7
private const val TAG = "StoryScreen"

@Composable
internal fun StoryScreen(
    levelId: String,
    unitId: String,
    modifier: Modifier = Modifier,
    viewModel: StoryViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val navBackStack = LocalNavBackStack.current
    val progressRepository: LearningProgressRepository = koinInject()
    val storyProgressRepository: LessonProgressRepository = koinInject()
    val levelRepository: LevelRepository = koinInject()
    val storyScope = rememberCoroutineScope()
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    var visibleSteps by remember(levelId) { mutableStateOf(DEFAULT_VISIBLE_STEPS) }
    LaunchedEffect(levelId) {
        visibleSteps = levelRepository.getVisibleSteps(levelId)
    }
    val stepSegments = remember(visibleSteps) {
        (visibleSteps + STORY_SEGMENT_INDEX).toImmutableList()
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }

    // Lối ra khi KHÔNG dựng được màn truyện — cấp chưa sinh `stories/level_N.json` thì
    // `StoryRepository.loadStories` trả rỗng, `storyForUnit` trả null, và screenState là Error.
    //
    // Không có nó thì `ErrorView` hiện ra KHÔNG MỘT NÚT NÀO (cả `retryAction` lẫn `terminate`
    // đều null), mà mọi lesson cuối của mọi unit đều dẫn tới đây → bé kẹt cứng. Truyền
    // `terminate` là đưa lại nút quay về Lesson Map.
    //
    // CỐ Ý không tự nhảy sang game và không tự đánh dấu `STORY_PROGRESS_ID` hoàn thành: thiếu
    // truyện là thiếu NỘI DUNG, nhảy qua êm ru thì ship cả cấp không có truyện cũng chẳng ai
    // hay. Mini Games vẫn khoá — đó là tín hiệu, không phải lỗi thứ hai.
    val backToLessonMap: () -> Unit = {
        while (navBackStack.size > 1 && navBackStack.last() !is Destination.Learning.LessonMap) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
        if (navBackStack.lastOrNull() !is Destination.Learning.LessonMap) {
            navBackStack.add(Destination.Learning.LessonMap(levelId, unitId))
        }
    }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
        terminate = backToLessonMap,
    ) { uiState ->
        val totalLessons = uiState.lessons.size
        val lastLessonIndex = (totalLessons - 1).coerceAtLeast(0)
        val perLessonSteps = visibleSteps.size

        LaunchedEffect(levelId, unitId, totalLessons, perLessonSteps) {
            if (totalLessons > 0 && perLessonSteps > 0) {
                val unitStepsTotal = totalLessons * perLessonSteps + 1
                val completedSteps = totalLessons * perLessonSteps
                val progressPercent = (completedSteps * 100) / unitStepsTotal
                progressRepository.setActivePosition(
                    levelId = levelId,
                    unitId = unitId,
                    lessonIndex = lastLessonIndex,
                    stepIndex = UNIT_STORY_STEP_INDEX,
                    progressPercent = progressPercent,
                )
            }
        }

        val onClose: () -> Unit = {
            val bookIdx = navBackStack.indexOfLast { it is Destination.Learning.UnitSelection }
            if (bookIdx >= 0) {
                while (navBackStack.size > bookIdx + 1) navBackStack.removeAt(navBackStack.lastIndex)
            } else {
                navBackStack.removeAt(navBackStack.lastIndex)
            }
        }
        val onNext: () -> Unit = {
            // Đánh dấu đã đọc xong story → Mini Games mở khoá trên Lesson Map.
            storyScope.launch { storyProgressRepository.markCompleted(STORY_PROGRESS_ID, unitId) }
            navBackStack.add(Destination.Learning.UnitGame(levelId, unitId, gameIndex = 0))
        }
        val onStepJump: (Int) -> Unit = { targetStep ->
            if (targetStep in visibleSteps) {
                Napier.d(tag = TAG) {
                    "Jump from UnitStory to Step($lastLessonIndex,$targetStep)"
                }
                if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                navBackStack.add(
                    Destination.Learning.Step(levelId, unitId, lastLessonIndex, targetStep),
                )
            }
        }

        StoryContent(
            story = uiState.story,
            scenes = uiState.scenes,
            audioState = audioState,
            sceneCompleted = viewModel.sceneCompleted,
            onPageChange = viewModel::onPageChange,
            onListen = viewModel::onListenToggle,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

@Composable
private fun StoryContent(
    story: Story,
    scenes: ImmutableList<StoryScene>,
    audioState: AudioState,
    sceneCompleted: SharedFlow<Int>,
    onPageChange: (Int) -> Unit,
    onListen: () -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    if (scenes.isEmpty()) return

    val pageCount = scenes.size
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val currentPage by remember { derivedStateOf { pagerState.currentPage.coerceIn(0, scenes.lastIndex) } }
    val scope = rememberCoroutineScope()
    val guideAudio = rememberAudioSession()
    val lang = LocalAppLanguage.current
    var guidePlayed by remember(story.id) { mutableStateOf(false) }

    // Auto-play scene audio when user swipes/lands on a new page (including first entry).
    // On first entry, play the spoken guide first, then the scene narration (no overlap).
    LaunchedEffect(currentPage) {
        if (currentPage == 0 && !guidePlayed) {
            guidePlayed = true
            guideAudio.playAndAwait(AudioRef.Prompt("vp_step_story", lang), STORY_GUIDE_MAX_MS)
        }
        onPageChange(currentPage)
    }

    // Auto-advance to next slide after the current scene's audio finishes (not user-stopped,
    // not last slide). Brief 800ms delay so the kid sees the last karaoke word before turning.
    LaunchedEffect(sceneCompleted, pagerState) {
        sceneCompleted.collect { sceneIndex ->
            if (pagerState.currentPage == sceneIndex && sceneIndex < scenes.lastIndex) {
                delay(AUTO_ADVANCE_DELAY_MS)
                if (pagerState.currentPage == sceneIndex) {
                    pagerState.animateScrollToPage(sceneIndex + 1)
                }
            }
        }
    }

    // Gate Next: only enabled once the kid has paged through to the last story slide.
    // Latched so it stays enabled if they swipe back to re-read earlier pages.
    var reachedLastPage by remember(story.title) { mutableStateOf(false) }
    LaunchedEffect(currentPage) {
        if (currentPage >= scenes.lastIndex) reachedLastPage = true
    }

    val currentScene = scenes[currentPage]
    // Kênh phát là kênh dùng chung: lúc mới vào màn nó đang đọc lời dẫn `vp_step_story`, và
    // khi lật trang nó còn đang đóng đuôi cảnh cũ. "Có tiếng nào đó đang phát" KHÔNG phải là
    // "cảnh này đang được đọc" — hỏi đúng ref của cảnh, nếu không chữ sẽ chạy theo lời dẫn.
    val sceneRef = remember(story.id, currentScene.sceneNumber) {
        AudioRef.Story(storyId = story.id, sceneNumber = currentScene.sceneNumber)
    }
    val isNarrating = audioState.isActiveFor(sceneRef)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                currentStepIndex = UNIT_STORY_STEP_INDEX,
                stepSegments = stepSegments,
                onClose = onClose,
                onStepJump = onStepJump,
                showSegments = false,
                guideText = stringResource(Res.string.step_guide_story),
                guideTrailing = {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PulseRings(
                            isActive = isNarrating,
                            ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        )
                        IconButton(
                            onClick = onListen,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(Res.string.story_audio_cd),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            StepContinueButton(
                label = stringResource(Res.string.common_next),
                onClick = onNext,
                enabled = reachedLastPage,
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
            Text(
                text = story.title,
                fontFamily = LocalPhonicsFontFamily.current,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            StorySceneImagePager(
                scenes = scenes,
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
                            (pagerState.currentPage + 1).coerceAtMost(scenes.lastIndex),
                        )
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            PageDotsRow(currentPage = currentPage, total = pageCount)
            Spacer(Modifier.height(16.dp))
            val positionMs = when (val s = audioState) {
                is AudioState.Playing -> if (s.ref == sceneRef) s.positionMs else -1L
                is AudioState.Paused -> if (s.ref == sceneRef) s.positionMs else -1L
                else -> -1L
            }
            // Chỉ trạng thái Playing mới cho chữ chạy. Loading cũng là "đang hoạt động" nhưng
            // chưa có mốc thời gian nào, mà KaraokeText hết mốc thì rơi về chạy theo nhịp cố
            // định — đúng cái cảnh chữ chạy trong khi chưa nghe thấy gì.
            val isReadingAloud = (audioState as? AudioState.Playing)?.ref == sceneRef
            KaraokeText(
                text = currentScene.text,
                isPlaying = isReadingAloud,
                positionMs = positionMs,
                wordTimings = currentScene.wordTimings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
@Suppress("UnstableCollections")
private fun StorySceneImagePager(
    scenes: List<StoryScene>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    val canPrev = pagerState.currentPage > 0
    val canNext = pagerState.currentPage < scenes.lastIndex

    Box(modifier = Modifier.fillMaxWidth()) {
        StoryStyleCard(aspectRatio = null, whiteInner = true) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) { pageIndex ->
                StorySceneCard(scene = scenes[pageIndex])
            }
        }

        StepChevronButton(
            icon = Icons.Filled.ChevronLeft,
            contentDescription = stringResource(Res.string.slide_previous_cd),
            enabled = canPrev,
            onClick = onPreviousPage,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
        )
        StepChevronButton(
            icon = Icons.Filled.ChevronRight,
            contentDescription = stringResource(Res.string.slide_next_cd),
            enabled = canNext,
            onClick = onNextPage,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
        )
    }
}

@Composable
private fun StorySceneCard(scene: StoryScene) {
    val contentBytes: ContentBytes = koinInject()
    val imagePath = scene.imagePathLandscape
    val bitmap: ImageBitmap? = if (imagePath != null) {
        produceState<ImageBitmap?>(initialValue = null, imagePath) {
            // Scene art moves out of the app with its unit's content pack, so it may live in
            // the APK, in the downloaded pack, or still be on the CDN — ContentBytes picks.
            // Decode off the Main recompose dispatcher — decodeToImageBitmap() is synchronous CPU work.
            value = withContext(Dispatchers.Default) {
                contentBytes.load(imagePath)?.let { bytes ->
                    runCatching { bytes.decodeToImageBitmap() }
                        .onFailure { Napier.w(tag = TAG) { "Undecodable image at $imagePath" } }
                        .getOrNull()
                }
            }
        }.value
    } else {
        null
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = scene.text,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = sceneEmoji(scene.name),
                fontSize = 120.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun sceneEmoji(name: String): String = when (name.lowercase()) {
    "intro" -> "📖"
    "problem" -> "🤔"
    "solution" -> "💡"
    "ending" -> "✨"
    else -> "🎬"
}

private const val AUTO_ADVANCE_DELAY_MS = 800L
private const val STORY_GUIDE_MAX_MS = 6_000L
