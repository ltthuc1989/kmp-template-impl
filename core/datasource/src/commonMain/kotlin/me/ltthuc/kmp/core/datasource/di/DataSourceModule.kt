package me.ltthuc.kmp.core.datasource.di

import me.ltthuc.kmp.core.datasource.AppSettingDataSource
import me.ltthuc.kmp.core.datasource.db.DatabaseSeeder
import me.ltthuc.kmp.core.datasource.db.GrabeeDatabase
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
    single { get<GrabeeDatabase>().unitCompletionDao() }
    single { get<GrabeeDatabase>().lessonProgressDao() }
    single { DatabaseSeeder(get(), get()) }

    includes(dataSourcePlatformModule)
}

internal expect val dataSourcePlatformModule: Module
