package me.ltthuc.kmp.core.repository.di

import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.BillingRepository
import me.ltthuc.kmp.core.repository.LearningProgressRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.UnitRepository
import me.ltthuc.kmp.core.repository.UserProgressRepository
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
