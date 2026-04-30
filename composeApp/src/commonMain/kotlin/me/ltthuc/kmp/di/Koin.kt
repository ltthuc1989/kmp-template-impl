package me.ltthuc.kmp.di

import me.ltthuc.kmp.core.billing.di.billingModule
import me.ltthuc.kmp.core.common.di.commonModule
import me.ltthuc.kmp.core.datasource.di.dataSourceModule
import me.ltthuc.kmp.core.repository.di.repositoryModule
import me.ltthuc.kmp.feature.billing.di.billingFeatureModule
import me.ltthuc.kmp.feature.home.di.homeModule
import me.ltthuc.kmp.feature.learningpath.di.learningPathModule
import me.ltthuc.kmp.feature.onboarding.di.onboardingModule
import me.ltthuc.kmp.feature.review.di.reviewModule
import me.ltthuc.kmp.feature.setting.di.settingModule
import org.koin.core.KoinApplication

fun KoinApplication.applyModules() {
    modules(appModule)

    modules(commonModule)
    modules(billingModule)
    modules(dataSourceModule)
    modules(repositoryModule)

    modules(homeModule)
    modules(settingModule)
    modules(billingFeatureModule)
    modules(learningPathModule)
    modules(onboardingModule)
    modules(reviewModule)
}
