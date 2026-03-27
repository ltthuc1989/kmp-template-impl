package me.matsumo.grabee.feature.learningpath.di

import me.matsumo.grabee.feature.learningpath.LearningPathViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val learningPathModule = module {
    viewModelOf(::LearningPathViewModel)
}
