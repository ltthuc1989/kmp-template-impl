package me.ltthuc.kmp.feature.setting.di

import me.ltthuc.kmp.feature.setting.SettingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val settingModule = module {
    viewModelOf(::SettingViewModel)
}
