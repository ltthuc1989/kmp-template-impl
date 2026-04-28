package me.matsumo.grabee.core.datasource.di

import me.matsumo.grabee.core.datasource.AppSettingDataSource
import me.matsumo.grabee.core.datasource.db.DatabaseSeeder
import me.matsumo.grabee.core.datasource.db.GrabeeDatabase
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataSourceModule = module {
    singleOf(::AppSettingDataSource)

    single { get<GrabeeDatabase>().levelDao() }
    single { get<GrabeeDatabase>().unitDao() }
    single { get<GrabeeDatabase>().phonicsLessonDao() }
    single { get<GrabeeDatabase>().learningProgressDao() }
    single { get<GrabeeDatabase>().userProgressDao() }
    single { DatabaseSeeder(get()) }

    includes(dataSourcePlatformModule)
}

internal expect val dataSourcePlatformModule: Module
