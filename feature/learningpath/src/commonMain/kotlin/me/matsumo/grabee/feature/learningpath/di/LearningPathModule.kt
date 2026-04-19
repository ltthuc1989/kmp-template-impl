package me.matsumo.grabee.feature.learningpath.di

import me.matsumo.grabee.feature.learningpath.UnitSelectionViewModel
import me.matsumo.grabee.feature.learningpath.step.StepNavigatorViewModel
import me.matsumo.grabee.feature.learningpath.step.chant.ChantViewModel
import me.matsumo.grabee.feature.learningpath.step.soundintro.SoundIntroViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val learningPathModule = module {
    viewModel { params -> UnitSelectionViewModel(levelId = params.get(), get(), get()) }
    viewModel { params -> StepNavigatorViewModel(unitId = params.get(), get()) }
    viewModel { params -> SoundIntroViewModel(unitId = params.get(), get()) }
    viewModel { params -> ChantViewModel(unitId = params.get(), get()) }
}
