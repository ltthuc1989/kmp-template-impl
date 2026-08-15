package me.ltthuc.kmp.feature.download.di

import me.ltthuc.kmp.feature.download.DownloadViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val downloadModule = module {
    viewModel { (levelId: String) -> DownloadViewModel(levelId, get()) }
}
