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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import me.matsumo.grabee.core.repository.AppSettingRepository
import me.matsumo.grabee.core.repository.LearningProgressRepository
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack
import me.matsumo.grabee.feature.learningpath.step.blending.BlendingScreen
import me.matsumo.grabee.feature.learningpath.step.chant.ChantScreen
import me.matsumo.grabee.feature.learningpath.step.identify.IdentifyScreen
import me.matsumo.grabee.feature.learningpath.step.matching.MatchingScreen
import me.matsumo.grabee.feature.learningpath.step.soundintro.SoundIntroScreen
import me.matsumo.grabee.feature.learningpath.step.tracing.TracingScreen
import me.matsumo.grabee.feature.learningpath.step.vocabulary.VocabularyScreen
import org.koin.compose.koinInject

// Each lesson has 7 step screens (0..6). Story (segment 7) lives at unit-level only — appears as
// the 8th segment on the last lesson and as a standalone destination after the last Tracing.
private const val PER_LESSON_STEPS = 7
private const val LAST_STEP_INDEX = PER_LESSON_STEPS - 1
private const val UNIT_STORY_SEGMENT_INDEX = PER_LESSON_STEPS    // = 7
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
    val progressRepository: LearningProgressRepository = koinInject()
    val settingRepository: AppSettingRepository = koinInject()
    val setting by settingRepository.setting.collectAsStateWithLifecycle()

    // totalLessons is reported back from each step screen's loaded data (its own VM already
    // has lessons by the time AsyncLoadContents renders the content block). This avoids the
    // race where a separate StepNavigatorViewModel might still have totalLessons=0.
    var totalLessons by remember(unitId) { mutableStateOf(0) }
    val onLessonsLoaded: (Int) -> Unit = { count ->
        if (count > 0 && count != totalLessons) totalLessons = count
    }

    val isLastLesson = totalLessons > 0 && lessonIndex == totalLessons - 1
    // Last lesson shows 8 segments (the extra one routes to UnitStory); other lessons show 7.
    val totalStepsForBar = if (isLastLesson) PER_LESSON_STEPS + 1 else PER_LESSON_STEPS

    LaunchedEffect(levelId, unitId, lessonIndex, stepIndex, totalLessons) {
        if (totalLessons > 0) {
            val unitStepsTotal = totalLessons * PER_LESSON_STEPS + 1
            val completedSteps = lessonIndex * PER_LESSON_STEPS + stepIndex
            val progressPercent = (completedSteps * 100) / unitStepsTotal
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
        Napier.d(tag = TAG) {
            "onNext invoked: $levelId/$unitId lesson=$lessonIndex step=$stepIndex " +
                "totalLessons=$totalLessons isLastLesson=$isLastLesson"
        }
        val next: Destination = when {
            stepIndex < LAST_STEP_INDEX ->
                Destination.Learning.Step(levelId, unitId, lessonIndex, stepIndex + 1)
            // We're on Tracing (last per-lesson step). Decide based on whether this is the last lesson.
            // If totalLessons hasn't loaded yet, optimistically advance the lesson — the next StepScreen
            // will coerce/clamp on its own data.
            isLastLesson ->
                Destination.Learning.UnitStory(levelId, unitId)
            else ->
                Destination.Learning.Step(levelId, unitId, lessonIndex + 1, stepIndex = 0)
        }
        Napier.d(tag = TAG) {
            "onNext advance: $levelId/$unitId lesson=$lessonIndex step=$stepIndex -> $next " +
                "(navBackStack size=${navBackStack.size})"
        }
        navBackStack.add(next)
    }
    val onClose: () -> Unit = {
        val bookIdx = navBackStack.indexOfLast { it is Destination.Learning.UnitSelection }
        if (bookIdx >= 0) {
            while (navBackStack.size > bookIdx + 1) navBackStack.removeAt(navBackStack.lastIndex)
        } else {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
    }
    val onPrevious: () -> Unit = {
        if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.size - 1)
    }
    val onStepJump: (Int) -> Unit = { targetStep ->
        when {
            targetStep == stepIndex -> Unit
            targetStep == UNIT_STORY_SEGMENT_INDEX && isLastLesson -> {
                if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                navBackStack.add(Destination.Learning.UnitStory(levelId, unitId))
            }
            targetStep in 0 until PER_LESSON_STEPS -> {
                if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                navBackStack.add(Destination.Learning.Step(levelId, unitId, lessonIndex, targetStep))
            }
        }
    }

    when (stepIndex) {
        0 -> SoundIntroScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        1 -> ChantScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
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
            showSpeakButton = setting.showSpeakButton,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
        )
        3 -> IdentifyScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        4 -> BlendingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        5 -> MatchingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        6 -> TracingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onPrevious = onPrevious,
            onNext = onNext,
            onStepJump = onStepJump,
            totalSteps = totalStepsForBar,
            onLessonsLoaded = onLessonsLoaded,
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
                title = { Text("Lesson ${lessonIndex + 1} · Step ${stepIndex + 1}/$PER_LESSON_STEPS — $stepName") },
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
    else -> "Unknown Step $stepIndex"
}
