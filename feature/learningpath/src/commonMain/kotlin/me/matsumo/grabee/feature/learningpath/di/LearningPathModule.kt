package me.matsumo.grabee.feature.learningpath.di

import me.matsumo.grabee.feature.learningpath.UnitSelectionViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val learningPathModule = module {
    viewModel { params -> UnitSelectionViewModel(levelId = params.get(), get(), get()) }
}
