package me.matsumo.grabee.di

import me.matsumo.grabee.core.billing.di.billingModule
import me.matsumo.grabee.core.common.di.commonModule
import me.matsumo.grabee.core.datasource.di.dataSourceModule
import me.matsumo.grabee.core.repository.di.repositoryModule
import me.matsumo.grabee.feature.billing.di.billingFeatureModule
import me.matsumo.grabee.feature.home.di.homeModule
import me.matsumo.grabee.feature.setting.di.settingModule
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
}
