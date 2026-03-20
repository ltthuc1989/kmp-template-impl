package me.matsumo.grabee.core.repository.di

import me.matsumo.grabee.core.repository.AppSettingRepository
import me.matsumo.grabee.core.repository.BillingRepository
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AppSettingRepository)
    singleOf(::BillingRepository)
}
