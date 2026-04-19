package me.matsumo.grabee.feature.learningpath.step.chant

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import me.matsumo.grabee.core.model.Word
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.chant_instruction
import me.matsumo.grabee.core.resource.chant_next
import me.matsumo.grabee.core.resource.chant_previous
import me.matsumo.grabee.core.resource.chant_title
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PuffySurface
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.abs
import kotlin.math.min

@Composable
internal fun ChantScreen(
    unitId: String,
    wordIndex: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChantViewModel = koinViewModel { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val safeIndex = wordIndex.coerceIn(0, uiState.words.lastIndex)
        ChantContent(
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
private fun ChantContent(
    currentWord: Word,
    words: ImmutableList<Word>,
    currentIndex: Int,
    totalWords: Int,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var isChanting by remember(currentWord.id) { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                title = stringResource(Res.string.chant_title),
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
            ChantHeroCard(word = currentWord, isChanting = isChanting)
            Spacer(Modifier.weight(1f, fill = true))
            PlayStopButton(
                isChanting = isChanting,
                onToggle = {
                    isChanting = !isChanting
                    // TODO: when audio ready — start/stop audio playback here
                },
            )
            Spacer(Modifier.height(16.dp))
            NavRow(onPrevious = onPrevious, onNext = onNext)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ChantHeroCard(word: Word, isChanting: Boolean) {
    PuffySurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(56.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 28.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.30f,
        topHighlightHeight = 20.dp,
        topHighlightAlpha = 0.9f,
        bottomShadeHeight = 20.dp,
        bottomShadeAlpha = 0.15f,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CharacterArtwork(word = word)
            Spacer(Modifier.height(20.dp))
            ChantText(chant = word.chantString(), isChanting = isChanting)
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
private fun CharacterArtwork(word: Word) {
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
            text = word.emoji.orEmpty().ifEmpty { "🎨" },
            fontSize = 96.sp,
        )
    }
}

/**
 * Karaoke-style chant animation: splits the chant string into syllable tokens,
 * then cycles a phase float through the tokens so each syllable briefly enlarges
 * and brightens as its "turn" comes. Animation only runs when [isChanting] is true;
 * otherwise shows static text with all tokens at `primary` color, base font size.
 *
 * When audio is wired later, drive [isChanting] from audio playback state and
 * replace the InfiniteTransition with an audio-position driven Float.
 */
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
                // Static view: all tokens at full active style (no animation).
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

@Composable
private fun NavRow(onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PreviousButton(
            onClick = onPrevious,
            modifier = Modifier.weight(1f),
        )
        NextButton(
            onClick = onNext,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PreviousButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.primary),
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 10.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.25f,
        topHighlightHeight = 8.dp,
        topHighlightAlpha = 0.8f,
        bottomShadeHeight = 8.dp,
        bottomShadeAlpha = 0.08f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.chant_previous),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun NextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onPrimary),
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 14.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.55f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.3f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.30f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.chant_next),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun Word.chantString(): String {
    val letter = text.firstOrNull() ?: return text
    return "${letter.uppercaseChar()}-${letter.lowercaseChar()}-${text.lowercase()}..."
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
