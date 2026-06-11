package me.ltthuc.kmp.feature.learningpath

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.lesson_complete_back_to_list
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.common.ConfettiCanvas
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun LessonCompleteScreen(
    levelId: String,
    unitId: String,
    lessonIndex: Int,
    modifier: Modifier = Modifier,
    viewModel: LessonCompleteViewModel = koinViewModel(
        key = "$unitId-$lessonIndex",
    ) { parametersOf(unitId, lessonIndex) },
) {
    val navBackStack = LocalNavBackStack.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = ScreenBg,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AsyncLoadContents(
                modifier = Modifier.fillMaxSize(),
                screenState = state,
            ) { ui ->
                LessonCompleteContent(
                    currentLetter = ui.currentLetter,
                    currentEmoji = ui.currentEmoji,
                    nextLetter = ui.nextLetter,
                    onNextLessonClick = {
                        if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                        navBackStack.add(
                            Destination.Learning.Step(
                                levelId = levelId,
                                unitId = unitId,
                                lessonIndex = lessonIndex + 1,
                                stepIndex = 0,
                            ),
                        )
                    },
                    onBackToLessonsClick = {
                        // Replace this completion screen with the unit's Lesson Map.
                        if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                        navBackStack.add(Destination.Learning.LessonMap(levelId, unitId))
                    },
                )
            }
        }
    }
}

@Composable
private fun LessonCompleteContent(
    currentLetter: String,
    currentEmoji: String?,
    nextLetter: String?,
    onNextLessonClick: () -> Unit,
    onBackToLessonsClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))

            StoryStyleCard(aspectRatio = null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Congratulations!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You finished letter $currentLetter",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                    if (currentEmoji != null) {
                        Spacer(Modifier.height(20.dp))
                        BouncingEmoji(emoji = currentEmoji)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NextLetterButton(
                    nextLetter = nextLetter,
                    onClick = onNextLessonClick,
                )
                Spacer(Modifier.height(12.dp))
                BackToLessonsButton(onClick = onBackToLessonsClick)
            }
        }
    }
}

@Composable
private fun BackToLessonsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
    ) {
        Text(
            text = stringResource(Res.string.lesson_complete_back_to_list),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun BouncingEmoji(emoji: String) {
    val transition = rememberInfiniteTransition(label = "lesson-complete-bounce")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = BOUNCE_TARGET_SCALE,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BOUNCE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    Text(
        text = emoji,
        fontSize = 56.sp,
        modifier = Modifier.scale(scale),
    )
}

@Composable
private fun NextLetterButton(nextLetter: String?, onClick: () -> Unit) {
    val label = if (nextLetter != null) "Learn letter $nextLetter" else "Continue"
    val interaction = remember { MutableInteractionSource() }
    PuffySurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
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
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private val ScreenBg = Color(0xFFFFF6E5)
private const val BOUNCE_DURATION_MS = 700
private const val BOUNCE_TARGET_SCALE = 1.18f
