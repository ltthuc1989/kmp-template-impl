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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack
import me.matsumo.grabee.feature.learningpath.step.blending.BlendingScreen
import me.matsumo.grabee.feature.learningpath.step.chant.ChantScreen
import me.matsumo.grabee.feature.learningpath.step.identify.IdentifyScreen
import me.matsumo.grabee.feature.learningpath.step.matching.MatchingScreen
import me.matsumo.grabee.feature.learningpath.step.soundintro.SoundIntroScreen
import me.matsumo.grabee.feature.learningpath.step.tracing.TracingScreen
import me.matsumo.grabee.feature.learningpath.step.vocabulary.VocabularyScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TOTAL_STEPS = 8

@Composable
internal fun StepScreen(
    levelId: String,
    unitId: String,
    wordIndex: Int,
    stepIndex: Int,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val navigator: StepNavigatorViewModel = koinViewModel { parametersOf(unitId) }
    val totalWords by navigator.totalWords.collectAsStateWithLifecycle()

    val onNext: () -> Unit = {
        if (totalWords > 0) {
            val next: Destination = when {
                stepIndex < TOTAL_STEPS - 1 ->
                    Destination.Learning.Step(levelId, unitId, wordIndex, stepIndex + 1)
                wordIndex < totalWords - 1 ->
                    Destination.Learning.Step(levelId, unitId, wordIndex + 1, stepIndex = 0)
                else ->
                    Destination.Learning.UnitComplete(levelId, unitId, starsEarned = 24)
            }
            navBackStack.add(next)
        }
    }
    val onClose: () -> Unit = { navBackStack.removeAt(navBackStack.size - 1) }
    val onPrevious: () -> Unit = {
        if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.size - 1)
    }

    when (stepIndex) {
        0 -> SoundIntroScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onNext = onNext,
            modifier = modifier,
        )
        1 -> ChantScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = modifier,
        )
        2 -> VocabularyScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = modifier,
        )
        3 -> IdentifyScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = modifier,
        )
        4 -> BlendingScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = modifier,
        )
        5 -> MatchingScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = modifier,
        )
        6 -> TracingScreen(
            unitId = unitId,
            wordIndex = wordIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            modifier = modifier,
        )
        else -> StepStubScreen(
            stepIndex = stepIndex,
            wordIndex = wordIndex,
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
    wordIndex: Int,
    stepName: String,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Letter ${wordIndex + 1} · Step ${stepIndex + 1}/$TOTAL_STEPS — $stepName") },
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
