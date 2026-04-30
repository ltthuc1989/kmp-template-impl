package me.matsumo.grabee.feature.learningpath.step.chant

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import me.matsumo.grabee.core.model.PhonicsLesson
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.chant_instruction
import me.matsumo.grabee.core.resource.chant_next
import me.matsumo.grabee.core.resource.chant_previous
import me.matsumo.grabee.core.resource.chant_title
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PageDotsRow
import me.matsumo.grabee.feature.learningpath.step.common.PuffySurface
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import me.matsumo.grabee.feature.learningpath.step.common.StepNavRow
import me.matsumo.grabee.feature.learningpath.step.common.StoryStyleCard
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
    modifier: Modifier = Modifier,
    totalSteps: Int = 7,
    onLessonsLoaded: (Int) -> Unit = {},
    viewModel: ChantViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        LaunchedEffect(uiState.lessons.size) { onLessonsLoaded(uiState.lessons.size) }
        val safeIndex = lessonIndex.coerceIn(0, uiState.lessons.lastIndex)
        ChantContent(
            currentLesson = uiState.lessons[safeIndex],
            lessons = uiState.lessons,
            currentIndex = safeIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalSteps,
        )
    }
}

@Composable
private fun ChantContent(
    currentLesson: PhonicsLesson,
    lessons: ImmutableList<PhonicsLesson>,
    currentIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onStepJump: (Int) -> Unit,
    totalSteps: Int,
) {
    var slideIndex by remember(currentLesson.id) { mutableIntStateOf(0) }
    var isChanting by remember(currentLesson.id) { mutableStateOf(false) }

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
                title = stringResource(Res.string.chant_title),
                currentStepIndex = STEP_INDEX,
                onClose = onClose,
                onStepJump = onStepJump,
                totalSteps = totalSteps,
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
            Spacer(Modifier.height(8.dp))
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
                    ChantCelebrationCard(
                        lesson = currentLesson,
                        isPlaying = isChanting,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PageDotsRow(currentPage = slideIndex, total = TOTAL_SLIDES)
            Spacer(Modifier.weight(1f, fill = true))
            PlayStopButton(
                isChanting = isChanting,
                onToggle = {
                    isChanting = !isChanting
                    if (isChanting && slideIndex == CELEBRATION_SLIDE_INDEX) {
                        slideIndex = 0
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
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
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.chant_instruction),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
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

@Composable
private fun PlayStopButton(
    isChanting: Boolean,
    onToggle: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .size(72.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary, bounded = false),
                onClick = onToggle,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 14.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.55f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.3f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.25f,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isChanting) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(36.dp),
            )
        }
    }
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
