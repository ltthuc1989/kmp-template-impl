package me.ltthuc.kmp.feature.learningpath

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.ForceEnglishLocale
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.game.GameFlowScreen
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.preview.BubblePopPreviewScreen
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.preview.MemoryMatchPreviewScreen
import me.ltthuc.kmp.feature.learningpath.step.StepScreen
import me.ltthuc.kmp.feature.learningpath.step.story.StoryScreen
import me.ltthuc.kmp.feature.learningpath.step.tracing.LetterGuideDebugScreen

// All learning screens are kid-facing → text forced to English ([ForceEnglishLocale]), regardless of
// the chosen app language. Resets the locale on entry so returning from a parent screen (which switched
// to the user's language) doesn't leak that language onto kid screens.
fun EntryProviderScope<NavKey>.learningEntry() {
    entry<Destination.Learning.UnitSelection> { dest ->
        ForceEnglishLocale {
            UnitSelectionScreen(
                levelId = dest.levelId,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.Step> { dest ->
        ForceEnglishLocale {
            StepScreen(
                levelId = dest.levelId,
                unitId = dest.unitId,
                lessonIndex = dest.lessonIndex,
                stepIndex = dest.stepIndex,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.LessonComplete> { dest ->
        ForceEnglishLocale {
            LessonCompleteScreen(
                levelId = dest.levelId,
                unitId = dest.unitId,
                lessonIndex = dest.lessonIndex,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.UnitStory> { dest ->
        ForceEnglishLocale {
            StoryScreen(
                levelId = dest.levelId,
                unitId = dest.unitId,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.UnitGame> { dest ->
        ForceEnglishLocale {
            GameFlowScreen(
                levelId = dest.levelId,
                unitId = dest.unitId,
                gameIndex = dest.gameIndex,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.LessonMap> { dest ->
        ForceEnglishLocale {
            LessonMapScreen(
                levelId = dest.levelId,
                unitId = dest.unitId,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.BubblePopPreview> {
        ForceEnglishLocale {
            BubblePopPreviewScreen(modifier = Modifier.fillMaxSize())
        }
    }
    entry<Destination.Learning.MemoryMatchPreview> {
        ForceEnglishLocale {
            MemoryMatchPreviewScreen(modifier = Modifier.fillMaxSize())
        }
    }
    entry<Destination.Learning.LevelComplete> { dest ->
        ForceEnglishLocale {
            LevelCompleteScreen(
                levelId = dest.levelId,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    entry<Destination.Learning.TracingGuideDebug> {
        val navBackStack = LocalNavBackStack.current
        ForceEnglishLocale {
            LetterGuideDebugScreen(
                onBack = { navBackStack.removeAt(navBackStack.lastIndex) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
