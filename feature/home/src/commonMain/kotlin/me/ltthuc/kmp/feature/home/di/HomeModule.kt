package me.ltthuc.kmp.feature.home.di

import me.ltthuc.kmp.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val homeModule = module {
    viewModelOf(::HomeViewModel)
}
