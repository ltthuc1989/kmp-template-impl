package me.ltthuc.kmp.feature.learningpath.step.chant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.audio.AudioState
import me.ltthuc.kmp.core.audio.isActiveFor
import me.ltthuc.kmp.core.model.ChantMeta
import me.ltthuc.kmp.core.model.PhonicsLesson
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.chant_listen_cd
import me.ltthuc.kmp.core.resource.step_guide_chant
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.feature.learningpath.step.common.LetterStepperBar
import me.ltthuc.kmp.feature.learningpath.step.common.PageDotsRow
import me.ltthuc.kmp.feature.learningpath.step.common.PulseRings
import me.ltthuc.kmp.feature.learningpath.step.common.StepHeader
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import me.ltthuc.kmp.feature.learningpath.step.common.chantRef
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.abs
import kotlin.math.min

private const val STEP_INDEX = 1
private const val TOTAL_SLIDES = 5
private const val CELEBRATION_SLIDE_INDEX = 4
private const val SLIDE_DURATION_MS = 2_500L

@Composable
internal fun ChantScreen(
    unitId: String,
    lessonIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
    modifier: Modifier = Modifier,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: ChantViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    val audioState by viewModel.audioState.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onLeaveScreen() }
    }

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        LaunchedEffect(uiState.lessons.size) { onLessonsLoaded(uiState.lessons.size) }
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        val currentLesson = uiState.lessons[safeIndex]
        val chantMeta by produceState<ChantMeta?>(initialValue = null, key1 = currentLesson.id) {
            value = viewModel.loadChantMeta(currentLesson)
        }
        ChantContent(
            currentLesson = currentLesson,
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            audioState = audioState,
            chantMeta = chantMeta,
            onChantToggle = { viewModel.onChantToggle(currentLesson) },
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
        )
    }
}

@Composable
private fun ChantContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    audioState: AudioState,
    chantMeta: ChantMeta?,
    onChantToggle: () -> Unit,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    stepSegments: ImmutableList<Int>,
) {
    var slideIndex by remember(currentLesson.id) { mutableIntStateOf(0) }
    val chantRef = remember(currentLesson.id) { currentLesson.chantRef() }
    val isChanting = chantRef != null && audioState.isActiveFor(chantRef)

    // Reset slide to 0 when chant starts fresh (i.e. transitions to playing while at celebration).
    LaunchedEffect(isChanting) {
        if (isChanting && slideIndex == CELEBRATION_SLIDE_INDEX) slideIndex = 0
    }

    // Auto-advance slides 0..3 while chanting; final celebration slide stays put.
    LaunchedEffect(currentLesson.id, slideIndex, isChanting) {
        if (isChanting && slideIndex < CELEBRATION_SLIDE_INDEX) {
            delay(SLIDE_DURATION_MS)
            slideIndex = (slideIndex + 1).coerceAtMost(CELEBRATION_SLIDE_INDEX)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                currentStepIndex = STEP_INDEX,
                onClose = onClose,
                onStepJump = onStepJump,
                onNext = onNext,
                nextEnabled = slideIndex >= CELEBRATION_SLIDE_INDEX,
                stepSegments = stepSegments,
                guideText = stringResource(Res.string.step_guide_chant),
                guideTrailing = {
                    Box(
                        modifier = Modifier.size(36.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        PulseRings(
                            isActive = isChanting,
                            ringColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        )
                        IconButton(
                            onClick = onChantToggle,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(Res.string.chant_listen_cd),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            LetterStepperBar(
                lessons = lessons,
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
            AnimatedContent(
                targetState = slideIndex,
                transitionSpec = {
                    (fadeIn(tween(300)) togetherWith fadeOut(tween(300)))
                        .using(SizeTransform(clip = false))
                },
                label = "chant-slide",
            ) { idx ->
                if (idx < CELEBRATION_SLIDE_INDEX) {
                    ChantHeroCard(
                        lesson = currentLesson,
                        wordIndex = idx,
                        isChanting = isChanting,
                    )
                } else {
                    val chantPositionMs = when (val s = audioState) {
                        is AudioState.Playing -> s.positionMs
                        is AudioState.Paused -> s.positionMs
                        else -> 0L
                    }
                    ChantCelebrationCard(
                        lesson = currentLesson,
                        isPlaying = isChanting,
                        audioPositionMs = chantPositionMs,
                        wordTimings = chantMeta?.wordTimings.orEmpty(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PageDotsRow(currentPage = slideIndex, total = TOTAL_SLIDES)
            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChantHeroCard(lesson: PhonicsLesson, wordIndex: Int, isChanting: Boolean) {
    val word = lesson.words.getOrNull(wordIndex)
    val chant = lesson.chantTexts.getOrNull(wordIndex)
        ?: lesson.stretchedWord
    StoryStyleCard(aspectRatio = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CharacterArtwork(emoji = word?.emoji.orEmpty())
            Spacer(Modifier.height(20.dp))
            ChantText(chant = chant, isChanting = isChanting)
        }
    }
}

@Composable
private fun CharacterArtwork(emoji: String) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.12f),
                spotColor = Color.Black.copy(alpha = 0.25f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji.ifEmpty { "🎨" },
            fontSize = 96.sp,
        )
    }
}

@Composable
private fun ChantText(chant: String, isChanting: Boolean) {
    val tokens = remember(chant) { chant.tokenize() }
    if (tokens.isEmpty()) return

    val transition = rememberInfiniteTransition(label = "chant")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = tokens.size.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = tokens.size * TOKEN_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val baseColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val activeColor = MaterialTheme.colorScheme.primary

    val annotated = buildAnnotatedString {
        tokens.forEachIndexed { i, token ->
            val proximity = if (isChanting) {
                val rawDist = abs(phase - i.toFloat())
                val wrappedDist = min(rawDist, tokens.size - rawDist)
                (1f - wrappedDist).coerceIn(0f, 1f)
            } else {
                1f
            }
            val fontSize = (BASE_FONT_SP + FONT_BUMP_SP * proximity).sp
            val color = lerp(baseColor, activeColor, proximity)
            withStyle(SpanStyle(color = color, fontSize = fontSize)) {
                append(token)
            }
            if (i < tokens.lastIndex) append(" ")
        }
    }

    Text(
        text = annotated,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        lineHeight = LINE_HEIGHT_SP.sp,
    )
}

private fun String.tokenize(): List<String> {
    return split(" ").flatMap { word ->
        if ("-" in word) {
            val parts = word.split("-")
            parts.mapIndexed { i, p -> if (i < parts.lastIndex) "$p-" else p }
                .filter { it.isNotEmpty() }
        } else {
            listOf(word)
        }
    }
}

private const val TOKEN_DURATION_MS = 450
private const val BASE_FONT_SP = 26f
private const val FONT_BUMP_SP = 6f
private const val LINE_HEIGHT_SP = 40f
