package me.matsumo.grabee.core.repository.di

import me.matsumo.grabee.core.repository.AppSettingRepository
import me.matsumo.grabee.core.repository.BillingRepository
import me.matsumo.grabee.core.repository.LearningProgressRepository
import me.matsumo.grabee.core.repository.LevelRepository
import me.matsumo.grabee.core.repository.UnitRepository
import me.matsumo.grabee.core.repository.UserProgressRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AppSettingRepository)
    singleOf(::BillingRepository)
    singleOf(::LearningProgressRepository)
    singleOf(::LevelRepository)
    singleOf(::UnitRepository)
    singleOf(::UserProgressRepository)
}
