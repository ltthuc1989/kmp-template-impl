package me.ltthuc.kmp.core.datasource.di

import android.content.Context
import me.ltthuc.kmp.core.datasource.db.GrabeeDatabase
import me.ltthuc.kmp.core.datasource.db.buildGrabeeDatabase
import me.ltthuc.kmp.core.datasource.db.createGrabeeDatabaseBuilder
import me.ltthuc.kmp.core.datasource.helper.PreferenceHelper
import me.ltthuc.kmp.core.datasource.helper.PreferenceHelperImpl
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val dataSourcePlatformModule: Module = module {
    single<PreferenceHelper> {
        PreferenceHelperImpl(
            context = get(),
            ioDispatcher = get(),
        )
    }
    single<GrabeeDatabase> {
        createGrabeeDatabaseBuilder(get<Context>()).buildGrabeeDatabase()
    }
}
