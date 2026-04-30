package me.ltthuc.kmp.core.billing.di

import me.ltthuc.kmp.core.billing.BillingDataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val billingModule = module {
    singleOf(::BillingDataSource)
}
