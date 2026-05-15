package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.ltthuc.kmp.core.repository.SfxController
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.level_complete_go_home
import me.ltthuc.kmp.core.resource.level_complete_letter_count
import me.ltthuc.kmp.core.resource.level_complete_parent_thanks
import me.ltthuc.kmp.core.resource.level_complete_replay
import me.ltthuc.kmp.core.resource.level_complete_subtitle
import me.ltthuc.kmp.core.resource.level_complete_title
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

@Composable
internal fun LevelCompleteScreen(
    levelId: String,
    modifier: Modifier = Modifier,
    viewModel: LevelCompleteViewModel = koinViewModel(key = levelId) { parametersOf(levelId) },
) {
    val navBackStack = LocalNavBackStack.current
    val sfx: SfxController = koinInject()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(levelId) {
        sfx.playSfx("lesson_complete")
        delay(LEVEL_COMPLETE_VOICE_DELAY_MS)
        sfx.playVoicePraise(LEVEL_COMPLETE_PRAISE_POOL.random())
    }

    Scaffold(
        modifier = modifier,
        containerColor = ScreenBgLevelComplete,
    ) { padding ->
        AsyncLoadContents(
            modifier = Modifier.fillMaxSize().padding(padding),
            screenState = state,
            containerColor = Color.Transparent,
        ) { ui ->
            CelebrationContent(
                levelTitle = ui.levelTitle,
                totalUnits = ui.totalUnits,
                onReplayClick = {
                    while (navBackStack.size > 0) {
                        navBackStack.removeAt(navBackStack.size - 1)
                    }
                    navBackStack.add(Destination.Home)
                    navBackStack.add(Destination.Learning.UnitSelection(levelId = levelId))
                    navBackStack.add(
                        Destination.Learning.Step(
                            levelId = levelId,
                            unitId = ui.firstUnitId,
                            lessonIndex = 0,
                            stepIndex = 0,
                        ),
                    )
                },
                onGoHomeClick = {
                    while (navBackStack.size > 0) {
                        navBackStack.removeAt(navBackStack.size - 1)
                    }
                    navBackStack.add(Destination.Home)
                },
            )
        }
    }
}

@Composable
private fun CelebrationContent(
    levelTitle: String,
    totalUnits: Int,
    onReplayClick: () -> Unit,
    onGoHomeClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        ConfettiCanvas(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))
            HeroCard(levelTitle = levelTitle, totalUnits = totalUnits)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.level_complete_parent_thanks),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(20.dp))
                ReplayButton(onClick = onReplayClick)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onGoHomeClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.level_complete_go_home),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(levelTitle: String, totalUnits: Int) {
    StoryStyleCard(aspectRatio = null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.level_complete_title),
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.level_complete_subtitle, levelTitle),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            LetterCountBadge(totalUnits = totalUnits)
        }
    }
}

@Composable
private fun LetterCountBadge(totalUnits: Int) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = stringResource(Res.string.level_complete_letter_count, totalUnits),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ReplayButton(onClick: () -> Unit) {
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.level_complete_replay),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private val ScreenBgLevelComplete = Color(0xFFFFF6E5)
private const val LEVEL_COMPLETE_VOICE_DELAY_MS = 800L
private val LEVEL_COMPLETE_PRAISE_POOL = listOf(
    "praise_great_job",
    "praise_well_done",
    "praise_you_got_it",
)
