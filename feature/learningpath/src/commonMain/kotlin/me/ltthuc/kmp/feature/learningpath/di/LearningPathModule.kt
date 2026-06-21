package me.ltthuc.kmp.feature.learningpath.di

import me.ltthuc.kmp.feature.learningpath.LessonCompleteViewModel
import me.ltthuc.kmp.feature.learningpath.LessonMapViewModel
import me.ltthuc.kmp.feature.learningpath.LevelCompleteViewModel
import me.ltthuc.kmp.feature.learningpath.UnitSelectionViewModel
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.BubblePopViewModel
import me.ltthuc.kmp.feature.learningpath.game.dragwords.DragWordsViewModel
import me.ltthuc.kmp.feature.learningpath.game.filletter.FillLetterViewModel
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.MemoryMatchViewModel
import me.ltthuc.kmp.feature.learningpath.game.pickword.PickWordViewModel
import me.ltthuc.kmp.feature.learningpath.game.spellletters.SpellLettersViewModel
import me.ltthuc.kmp.feature.learningpath.step.blending.BlendingViewModel
import me.ltthuc.kmp.feature.learningpath.step.chant.ChantViewModel
import me.ltthuc.kmp.feature.learningpath.step.identify.IdentifyViewModel
import me.ltthuc.kmp.feature.learningpath.step.matching.MatchingViewModel
import me.ltthuc.kmp.feature.learningpath.step.soundintro.SoundIntroViewModel
import me.ltthuc.kmp.feature.learningpath.step.story.StoryViewModel
import me.ltthuc.kmp.feature.learningpath.step.tracing.TracingViewModel
import me.ltthuc.kmp.feature.learningpath.step.vocabulary.VocabularyViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val learningPathModule = module {
    viewModel { params -> UnitSelectionViewModel(levelId = params.get(), get(), get()) }
    viewModel { params -> LevelCompleteViewModel(levelId = params.get(), get(), get()) }
    viewModel { params ->
        LessonCompleteViewModel(
            unitId = params.get(),
            lessonIndex = params.get(),
            unitRepository = get(),
        )
    }
    viewModel { params ->
        LessonMapViewModel(
            unitId = params.get(),
            unitRepository = get(),
            unitCompletionRepository = get(),
        )
    }
    viewModel { params -> SoundIntroViewModel(unitId = params.get(), get(), get()) }
    viewModel { params -> ChantViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> VocabularyViewModel(unitId = params.get(), get(), get()) }
    viewModel { params -> IdentifyViewModel(unitId = params.get(), get(), get()) }
    viewModel { params -> BlendingViewModel(unitId = params.get(), get(), get()) }
    viewModel { params -> MatchingViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> TracingViewModel(unitId = params.get(), get()) }
    viewModel { params -> StoryViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> BubblePopViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> MemoryMatchViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> PickWordViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> FillLetterViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> SpellLettersViewModel(unitId = params.get(), get(), get(), get()) }
    viewModel { params -> DragWordsViewModel(unitId = params.get(), get(), get(), get()) }
}
