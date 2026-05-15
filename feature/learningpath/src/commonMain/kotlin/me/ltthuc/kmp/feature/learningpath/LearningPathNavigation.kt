package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.step.StepScreen
import me.ltthuc.kmp.feature.learningpath.step.story.StoryScreen
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuideDebugScreen

fun EntryProviderScope<NavKey>.learningEntry() {
    entry<Destination.Learning.UnitSelection> { dest ->
        UnitSelectionScreen(
            levelId = dest.levelId,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<Destination.Learning.Step> { dest ->
        StepScreen(
            levelId = dest.levelId,
            unitId = dest.unitId,
            lessonIndex = dest.lessonIndex,
            stepIndex = dest.stepIndex,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<Destination.Learning.UnitComplete> { dest ->
        UnitCompleteScreen(
            levelId = dest.levelId,
            unitId = dest.unitId,
            starsEarned = dest.starsEarned,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<Destination.Learning.LessonComplete> { dest ->
        LessonCompleteScreen(
            levelId = dest.levelId,
            unitId = dest.unitId,
            lessonIndex = dest.lessonIndex,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<Destination.Learning.UnitStory> { dest ->
        StoryScreen(
            levelId = dest.levelId,
            unitId = dest.unitId,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<Destination.Learning.LevelComplete> { dest ->
        LevelCompleteScreen(
            levelId = dest.levelId,
            modifier = Modifier.fillMaxSize(),
        )
    }
    entry<Destination.Learning.TracingGuideDebug> {
        val navBackStack = LocalNavBackStack.current
        LetterGuideDebugScreen(
            onBack = { navBackStack.removeAt(navBackStack.lastIndex) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
