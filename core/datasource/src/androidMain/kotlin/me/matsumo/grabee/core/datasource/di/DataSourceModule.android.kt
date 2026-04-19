package me.matsumo.grabee.core.datasource.di

import android.content.Context
import me.matsumo.grabee.core.datasource.db.GrabeeDatabase
import me.matsumo.grabee.core.datasource.db.buildGrabeeDatabase
import me.matsumo.grabee.core.datasource.db.createGrabeeDatabaseBuilder
import me.matsumo.grabee.core.datasource.helper.PreferenceHelper
import me.matsumo.grabee.core.datasource.helper.PreferenceHelperImpl
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
