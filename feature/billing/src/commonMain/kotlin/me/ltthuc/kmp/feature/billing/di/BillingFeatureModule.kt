package me.ltthuc.kmp.feature.billing.di

import me.ltthuc.kmp.feature.billing.PaywallViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val billingFeatureModule = module {
    viewModelOf(::PaywallViewModel)
}
