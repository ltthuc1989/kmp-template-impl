package me.ltthuc.kmp.feature.billing.di

import me.ltthuc.kmp.feature.billing.PaywallViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val billingFeatureModule = module {
    viewModel { params -> PaywallViewModel(get(), get(), get(), params.getOrNull()) }
}
