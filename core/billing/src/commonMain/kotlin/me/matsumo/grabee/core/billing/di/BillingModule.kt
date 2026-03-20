package me.matsumo.grabee.core.billing.di

import me.matsumo.grabee.core.billing.BillingDataSource
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val billingModule = module {
    singleOf(::BillingDataSource)
}
