package me.ltthuc.kmp.feature.review.di

import me.ltthuc.kmp.feature.review.ReviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val reviewModule = module {
    viewModelOf(::ReviewViewModel)
}
