package me.ltthuc.kmp.feature.learningpath

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.model.PhonicsUnit
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.repository.UnitCompletionRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.unit_complete_choose_letter
import me.ltthuc.kmp.core.resource.unit_complete_level_done
import me.ltthuc.kmp.core.resource.unit_complete_next_button
import me.ltthuc.kmp.core.resource.unit_complete_practice_badge
import me.ltthuc.kmp.core.resource.unit_complete_subtitle
import me.ltthuc.kmp.core.resource.unit_complete_title
import me.ltthuc.kmp.core.ui.ads.BottomBannerAd
import me.ltthuc.kmp.core.ui.ads.LearningInterstitial
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.common.ConfettiCanvas
import me.ltthuc.kmp.feature.learningpath.step.common.PuffySurface
import me.ltthuc.kmp.feature.learningpath.step.common.StoryStyleCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun UnitCompleteScreen(
    levelId: String,
    unitId: String,
    starsEarned: Int,
    modifier: Modifier = Modifier,
    viewModel: UnitCompleteViewModel = koinViewModel(key = unitId) { parametersOf(unitId) },
) {
    val navBackStack = LocalNavBackStack.current
    val unitCompletionRepository: UnitCompletionRepository = koinInject()
    val sfx: SfxController = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Reaching this screen = user finished all lessons + Story. Increment unit completion
    // counter to drive next-unit unlock and the "Practiced N times" badge.
    LaunchedEffect(unitId) {
        unitCompletionRepository.markCompleted(unitId)
    }
    // Khan-simple celebration: fire fanfare on entry, then voice praise 800ms in.
    LaunchedEffect(unitId) {
        sfx.playSfx("lesson_complete")
        delay(UNIT_COMPLETE_VOICE_DELAY_MS)
        sfx.playVoicePraise(UNIT_COMPLETE_PRAISE_POOL.random())
    }

    LearningInterstitial()

    Scaffold(
        modifier = modifier,
        containerColor = ScreenBgComplete,
        bottomBar = { BottomBannerAd() },
    ) { padding ->
        AsyncLoadContents(
            modifier = Modifier.fillMaxSize().padding(padding),
            screenState = state,
        ) { ui ->
            CelebrationContent(
                title = ui.unit.title,
                emojis = ui.emojis,
                completionCount = ui.completionCount,
                showChooseLetter = ui.completedUnitCount >= MIN_UNITS_FOR_CHOOSE_LETTER,
                nextUnit = ui.nextUnit,
                onNextClick = {
                    while (
                        navBackStack.size > 0 &&
                        navBackStack.last() !is Destination.Learning.UnitSelection
                    ) {
                        navBackStack.removeAt(navBackStack.size - 1)
                    }
                    val nextUnit = ui.nextUnit
                    if (nextUnit != null) {
                        navBackStack.add(
                            Destination.Learning.Step(
                                levelId = levelId,
                                unitId = nextUnit.id,
                                lessonIndex = 0,
                                stepIndex = 0,
                            ),
                        )
                    } else {
                        // Last unit of last available level → celebrate level completion.
                        navBackStack.add(Destination.Learning.LevelComplete(levelId = levelId))
                    }
                },
                onChooseLetterClick = {
                    while (
                        navBackStack.size > 0 &&
                        navBackStack.last() !is Destination.Learning.UnitSelection
                    ) {
                        navBackStack.removeAt(navBackStack.size - 1)
                    }
                },
            )
        }
    }
    // [starsEarned] is a legacy nav param retained for back-compat with Destination.Learning.UnitComplete;
    // counter info now comes from UnitCompletionRepository via the ViewModel.
}

@Composable
private fun CelebrationContent(
    title: String,
    emojis: ImmutableList<String>,
    completionCount: Int,
    showChooseLetter: Boolean,
    nextUnit: PhonicsUnit?,
    onNextClick: () -> Unit,
    onChooseLetterClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Layer 1: Confetti rain (loops infinitely)
        ConfettiCanvas(modifier = Modifier.fillMaxSize())

        // Layer 2: Hero card + CTAs
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))
            HeroCard(
                title = title,
                emojis = emojis,
                completionCount = completionCount,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PrimaryNextButton(nextUnit = nextUnit, onClick = onNextClick)
                if (showChooseLetter) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onChooseLetterClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.unit_complete_choose_letter),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    emojis: ImmutableList<String>,
    completionCount: Int,
) {
    StoryStyleCard(aspectRatio = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.unit_complete_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.unit_complete_subtitle, title),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            if (emojis.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                BouncingEmojiRow(emojis = emojis)
            }
            Spacer(Modifier.height(20.dp))
            PracticeBadge(completionCount = completionCount)
        }
    }
}

@Composable
private fun BouncingEmojiRow(emojis: ImmutableList<String>) {
    val transition = rememberInfiniteTransition(label = "emoji-bounce")
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        emojis.forEachIndexed { index, emoji ->
            // Stagger phase per emoji so each letter bounces at a slightly different time —
            // visually playful, draws kid's eye across all 3 letters.
            val phaseOffsetMs = (index * BOUNCE_DURATION_MS / emojis.size.coerceAtLeast(1))
            val scale by transition.animateFloat(
                initialValue = 1f,
                targetValue = BOUNCE_TARGET_SCALE,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = BOUNCE_DURATION_MS,
                        delayMillis = phaseOffsetMs,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "scale-$index",
            )
            Text(
                text = emoji,
                fontSize = 44.sp,
                modifier = Modifier.scale(scale),
            )
        }
    }
}

@Composable
private fun PracticeBadge(completionCount: Int) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.unit_complete_practice_badge, completionCount),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun PrimaryNextButton(nextUnit: PhonicsUnit?, onClick: () -> Unit) {
    val label = if (nextUnit != null) {
        stringResource(Res.string.unit_complete_next_button, nextUnit.title)
    } else {
        stringResource(Res.string.unit_complete_level_done)
    }
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
            if (nextUnit != null) {
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
}

private val ScreenBgComplete = Color(0xFFFFF6E5)
private const val MIN_UNITS_FOR_CHOOSE_LETTER = 2
private const val BOUNCE_DURATION_MS = 700
private const val BOUNCE_TARGET_SCALE = 1.18f
private const val UNIT_COMPLETE_VOICE_DELAY_MS = 800L
private val UNIT_COMPLETE_PRAISE_POOL = listOf(
    "praise_great_job",
    "praise_well_done",
    "praise_you_got_it",
)
