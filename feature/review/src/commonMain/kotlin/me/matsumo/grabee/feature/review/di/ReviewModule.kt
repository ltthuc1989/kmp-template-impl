package me.matsumo.grabee.feature.review.di

import me.matsumo.grabee.feature.review.ReviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reviewModule = module {
    viewModelOf(::ReviewViewModel)
}
