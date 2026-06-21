package me.ltthuc.kmp.feature.learningpath.step

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LessonProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.blending.BlendingScreen
import me.ltthuc.kmp.feature.learningpath.step.chant.ChantScreen
import me.ltthuc.kmp.feature.learningpath.step.identify.IdentifyScreen
import me.ltthuc.kmp.feature.learningpath.step.matching.MatchingScreen
import me.ltthuc.kmp.feature.learningpath.step.soundintro.SoundIntroScreen
import me.ltthuc.kmp.feature.learningpath.step.tracing.TracingScreen
import me.ltthuc.kmp.feature.learningpath.step.vocabulary.VocabularyScreen
import org.koin.compose.koinInject

// Canonical step screens (0..6); each level may hide some via LevelEntity.visibleStepsJson.
// Story (the per-unit story screen) lives at unit-level only — appears as an extra trailing
// segment on the last lesson and as a standalone destination after the last visible step.
// STORY_SEGMENT_INDEX is a sentinel that comes after all canonical step indices.
internal const val MAX_STEP_INDEX = 6
internal const val STORY_SEGMENT_INDEX = MAX_STEP_INDEX + 1 // = 7
internal val DEFAULT_VISIBLE_STEPS = (0..MAX_STEP_INDEX).toList()
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
    val lessonProgressRepository: LessonProgressRepository = koinInject()
    val levelRepository: LevelRepository = koinInject()
    val unitRepository: UnitRepository = koinInject()
    val scope = rememberCoroutineScope()

    // Current lesson's stable id, used to mark the lesson completed on the last step.
    // StepScreen itself doesn't hold the lessons list (child step screens load them), so we
    // observe them here. lessonIndex maps to the sorted-by-orderIndex position.
    var currentLessonId by remember(unitId, lessonIndex) { mutableStateOf<String?>(null) }
    LaunchedEffect(unitId, lessonIndex) {
        unitRepository.observeLessons(unitId).collect { lessons ->
            currentLessonId = lessons.sortedBy { it.orderIndex }.getOrNull(lessonIndex)?.id
        }
    }

    var visibleSteps by remember(levelId) { mutableStateOf(DEFAULT_VISIBLE_STEPS) }
    LaunchedEffect(levelId) {
        visibleSteps = levelRepository.getVisibleSteps(levelId)
    }

    // totalLessons is reported back from each step screen's loaded data (its own VM already
    // has lessons by the time AsyncLoadContents renders the content block). This avoids the
    // race where a separate StepNavigatorViewModel might still have totalLessons=0.
    var totalLessons by remember(unitId) { mutableStateOf(0) }
    val onLessonsLoaded: (Int) -> Unit = { count ->
        if (count > 0 && count != totalLessons) totalLessons = count
    }

    val perLessonSteps = visibleSteps.size
    val lastVisibleStepIndex = visibleSteps.last()
    val isLastLesson = totalLessons > 0 && lessonIndex == totalLessons - 1
    // Canonical step indices to render in the StepHeader segment row. The Story is no longer
    // shown as a trailing segment — the last lesson's bar matches every other lesson's.
    val stepSegments = remember(visibleSteps) {
        visibleSteps.toImmutableList()
    }

    // Defensive: if user lands on a hidden step (e.g., resuming from old saved progress),
    // redirect forward to the next visible step. visibleSteps loads with default = all 7 so
    // this only fires after the actual config arrives.
    LaunchedEffect(visibleSteps, stepIndex) {
        if (stepIndex in 0..MAX_STEP_INDEX && stepIndex !in visibleSteps) {
            val target = visibleSteps.firstOrNull { it > stepIndex } ?: visibleSteps.first()
            Napier.d(tag = TAG) {
                "Redirect hidden step $stepIndex -> $target (visible=$visibleSteps)"
            }
            if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
            navBackStack.add(Destination.Learning.Step(levelId, unitId, lessonIndex, target))
        }
    }

    LaunchedEffect(levelId, unitId, lessonIndex, stepIndex, totalLessons, perLessonSteps) {
        if (totalLessons > 0 && perLessonSteps > 0) {
            val unitStepsTotal = totalLessons * perLessonSteps + 1
            val visiblePos = visibleSteps.indexOf(stepIndex).coerceAtLeast(0)
            val completedSteps = lessonIndex * perLessonSteps + visiblePos
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
                "visible=$visibleSteps totalLessons=$totalLessons isLastLesson=$isLastLesson"
        }
        val next: Destination = when {
            stepIndex < lastVisibleStepIndex -> {
                val nextStep = visibleSteps.firstOrNull { it > stepIndex } ?: lastVisibleStepIndex
                Destination.Learning.Step(levelId, unitId, lessonIndex, nextStep)
            }
            // We're on the last visible step. Decide based on whether this is the last lesson.
            // If totalLessons hasn't loaded yet, optimistically advance the lesson — the next
            // StepScreen will coerce/clamp on its own data.
            isLastLesson ->
                Destination.Learning.UnitStory(levelId, unitId)
            else ->
                Destination.Learning.LessonComplete(levelId, unitId, lessonIndex)
        }
        // Reaching the last visible step completes this lesson → record it so the Lesson Map
        // unlocks the next lesson (and Story/Mini Games once every lesson is done).
        if (stepIndex >= lastVisibleStepIndex) {
            currentLessonId?.let { id ->
                scope.launch { lessonProgressRepository.markCompleted(id, unitId) }
            }
        }
        Napier.d(tag = TAG) {
            "onNext advance: $levelId/$unitId lesson=$lessonIndex step=$stepIndex -> $next " +
                "(navBackStack size=${navBackStack.size})"
        }
        navBackStack.add(next)
    }
    // Close = exit the lesson straight back to the Lesson Map (pop the accumulated Step screens).
    val onClose: () -> Unit = {
        while (navBackStack.size > 1 && navBackStack.last() !is Destination.Learning.LessonMap) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
        if (navBackStack.lastOrNull() !is Destination.Learning.LessonMap) {
            navBackStack.add(Destination.Learning.LessonMap(levelId, unitId))
        }
    }
    val onStepJump: (Int) -> Unit = { targetStep ->
        when {
            targetStep == stepIndex -> Unit
            targetStep in visibleSteps -> {
                if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex)
                navBackStack.add(Destination.Learning.Step(levelId, unitId, lessonIndex, targetStep))
            }
        }
    }
    // Tracing (last step) completing a letter: mark the lesson done, then pop the accumulated
    // Step screens back to the Lesson Map so the kid lands straight on lesson select.
    val onTracingComplete: () -> Unit = {
        currentLessonId?.let { id ->
            scope.launch { lessonProgressRepository.markCompleted(id, unitId) }
        }
        while (navBackStack.size > 1 && navBackStack.last() !is Destination.Learning.LessonMap) {
            navBackStack.removeAt(navBackStack.lastIndex)
        }
        if (navBackStack.lastOrNull() !is Destination.Learning.LessonMap) {
            navBackStack.add(Destination.Learning.LessonMap(levelId, unitId))
        }
    }

    when (stepIndex) {
        0 -> SoundIntroScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        1 -> ChantScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        2 -> VocabularyScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            modifier = modifier,
            stepSegments = stepSegments,
            onLessonsLoaded = onLessonsLoaded,
        )
        3 -> IdentifyScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        4 -> BlendingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        5 -> MatchingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onNext,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
            onLessonsLoaded = onLessonsLoaded,
            modifier = modifier,
        )
        6 -> TracingScreen(
            unitId = unitId,
            lessonIndex = lessonIndex,
            onClose = onClose,
            onNext = onTracingComplete,
            onStepJump = onStepJump,
            stepSegments = stepSegments,
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
                title = { Text("Lesson ${lessonIndex + 1} · Step ${stepIndex + 1} — $stepName") },
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
