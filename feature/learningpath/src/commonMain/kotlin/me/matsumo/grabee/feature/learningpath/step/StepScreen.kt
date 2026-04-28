package me.matsumo.grabee.feature.learningpath.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import me.matsumo.grabee.core.repository.LearningProgressRepository
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack
import me.matsumo.grabee.feature.learningpath.step.blending.BlendingScreen
import me.matsumo.grabee.feature.learningpath.step.chant.ChantScreen
import me.matsumo.grabee.feature.learningpath.step.identify.IdentifyScreen
import me.matsumo.grabee.feature.learningpath.step.matching.MatchingScreen
import me.matsumo.grabee.feature.learningpath.step.soundintro.SoundIntroScreen
import me.matsumo.grabee.feature.learningpath.step.story.StoryScreen
import me.matsumo.grabee.feature.learningpath.step.tracing.TracingScreen
import me.matsumo.grabee.feature.learningpath.step.vocabulary.VocabularyScreen
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TOTAL_STEPS = 8
private const val TAG = "StepScreen"

@Composable
internal fun StepScreen(
    levelId: String,
    unitId: String,
    lessonIndex: Int,
    stepIndex: Int,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val navigator: StepNavigatorViewModel = koinViewModel(key = unitId) { parametersOf(unitId) }
    val totalLessons by navigator.totalLessons.collectAsStateWithLifecycle()
    val progressRepository: LearningProgressRepository = koinInject()

    LaunchedEffect(levelId, unitId, lessonIndex, stepIndex, totalLessons) {
        if (totalLessons > 0) {
            val unitStepsTotal = totalLessons * TOTAL_STEPS
            val completedSteps = lessonIndex * TOTAL_STEPS + stepIndex
            val progressPercent = if (unitStepsTotal > 0) {
                (completedSteps * 100) / unitStepsTotal
            } else {
                0
            }
            progressRepository.setActivePosition(
                levelId = levelId,
                unitId = unitId,
                lessonIndex = lessonIndex,
                stepIndex = stepIndex,
                progressPercent = progressPercent,
            )
        }
    }

    val onNext: () -> Unit = {
        val next: Destination? = when {
            stepIndex < TOTAL_STEPS - 1 ->
                Destination.Learning.Step(levelId, unitId, lessonIndex, stepIndex + 1)
            totalLessons <= 0 -> {
                Napier.w(tag = TAG) {
                    "onNext at last step but totalLessons=$totalLessons — waiting for flow " +
                        "(unit=$unitId, lesson=$lessonIndex)"
                }
                null
            }
            lessonIndex < totalLessons - 1 ->
                Destination.Learning.Step(levelId, unitId, lessonIndex + 1, stepIndex = 0)
            else ->
                Destination.Learning.UnitComplete(levelId, unitId, starsEarned = 24)
        }
        if (next != null) {
            Napier.d(tag = TAG) {
                "onNext advance: $levelId/$unitId lesson=$lessonIndex step=$stepIndex -> $next"
            }
            navBackStack.add(next)
        }
    }
    val onClose: () -> Unit = { navBackStack.removeAt(navBackStack.size - 1) }
    val onPrevious: () -> Unit = {
        if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.size - 1)
    }
    val onStepJump: (Int) -> Unit = { targetStep ->
        if (targetStep in 0 until TOTAL_STEPS && targetStep != stepIndex) {
            if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.size - 1)
            navBackStack.add(Destination.Learning.Step(levelId, unitId, lessonIndex, targetStep))
        }
    }

    when (stepIndex) {
        0 -> SoundIntroScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        1 -> ChantScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        2 -> VocabularyScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        3 -> IdentifyScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        4 -> BlendingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        5 -> MatchingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        6 -> TracingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        7 -> StoryScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
        )
        else -> StepStubScreen(
            stepIndex = stepIndex,
            lessonIndex = lessonIndex,
            stepName = stepName(stepIndex),
            onBack = onClose,
            onContinue = onNext,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepStubScreen(
    stepIndex: Int,
    lessonIndex: Int,
    stepName: String,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Lesson ${lessonIndex + 1} · Step ${stepIndex + 1}/$TOTAL_STEPS — $stepName") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Design pending — $stepName")
            Spacer(Modifier.height(24.dp))
            Button(onClick = onContinue) {
                Text("Continue")
            }
        }
    }
}

private fun stepName(stepIndex: Int): String = when (stepIndex) {
    0 -> "Sound Intro"
    1 -> "Chant"
    2 -> "Vocabulary"
    3 -> "Identify"
    4 -> "Blending"
    5 -> "Matching"
    6 -> "Tracing"
    7 -> "Story"
    else -> "Unknown Step $stepIndex"
}
