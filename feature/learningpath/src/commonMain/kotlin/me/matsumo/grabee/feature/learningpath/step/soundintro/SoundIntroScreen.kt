package me.matsumo.grabee.feature.learningpath.step.soundintro

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import me.matsumo.grabee.core.model.Word
import me.matsumo.grabee.core.resource.Res
import me.matsumo.grabee.core.resource.sound_intro_can_you_say
import me.matsumo.grabee.core.resource.sound_intro_instruction
import me.matsumo.grabee.core.resource.sound_intro_listen
import me.matsumo.grabee.core.resource.sound_intro_next_lesson
import me.matsumo.grabee.core.resource.sound_intro_title
import me.matsumo.grabee.core.resource.sound_intro_word_example
import me.matsumo.grabee.core.ui.screen.AsyncLoadContents
import me.matsumo.grabee.feature.learningpath.step.common.LetterStepperBar
import me.matsumo.grabee.feature.learningpath.step.common.PuffySurface
import me.matsumo.grabee.feature.learningpath.step.common.StepHeader
import me.matsumo.grabee.feature.learningpath.step.common.letterPair
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun SoundIntroScreen(
    unitId: String,
    wordIndex: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SoundIntroViewModel = koinViewModel { parametersOf(unitId) },
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    AsyncLoadContents(
        modifier = modifier.fillMaxSize(),
        screenState = screenState,
    ) { uiState ->
        val safeIndex = wordIndex.coerceIn(0, uiState.words.lastIndex)
        SoundIntroContent(
            currentWord = uiState.words[safeIndex],
            words = uiState.words,
            currentIndex = safeIndex,
            totalWords = uiState.words.size,
            onClose = onClose,
            onListen = { /* TODO: wire audio player when assets ready */ },
            onNext = onNext,
        )
    }
}

@Composable
private fun SoundIntroContent(
    currentWord: Word,
    words: ImmutableList<Word>,
    currentIndex: Int,
    totalWords: Int,
    onClose: () -> Unit,
    onListen: () -> Unit,
    onNext: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StepHeader(
                title = stringResource(Res.string.sound_intro_title),
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
            LetterHeroCard(
                word = currentWord,
                onListen = onListen,
            )
            Spacer(Modifier.weight(1f, fill = true))
            PromptSection(letterPair = currentWord.letterPair())
            Spacer(Modifier.height(16.dp))
            NextLessonButton(onClick = onNext)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LetterHeroCard(
    word: Word,
    onListen: () -> Unit,
) {
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
            Text(
                text = word.letterPair(),
                fontSize = 76.sp,
                lineHeight = 80.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PhonemePill(phoneme = word.phoneme)
                Text(
                    text = stringResource(Res.string.sound_intro_word_example, word.text),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(20.dp))
            CharacterArtwork(word = word)
            Spacer(Modifier.height(24.dp))
            PuffyListenButton(onClick = onListen)
        }
    }
}

@Composable
private fun PhonemePill(phoneme: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            text = phoneme,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
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

@Composable
private fun PuffyListenButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = MaterialTheme.colorScheme.onSecondaryContainer),
                onClick = onClick,
            ),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
        shadowTint = MaterialTheme.colorScheme.secondary,
        shadowAlpha = 0.7f,
        topHighlightHeight = 7.dp,
        topHighlightAlpha = 0.8f,
        bottomShadeHeight = 7.dp,
        bottomShadeAlpha = 0.15f,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.sound_intro_listen),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun PromptSection(letterPair: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.sound_intro_can_you_say, letterPair),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.sound_intro_instruction),
            fontSize = 16.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NextLessonButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
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
                text = stringResource(Res.string.sound_intro_next_lesson),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
