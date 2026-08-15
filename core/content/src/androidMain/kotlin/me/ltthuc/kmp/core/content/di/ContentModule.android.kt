package me.ltthuc.kmp.core.content.di

import android.content.Context
import me.ltthuc.kmp.core.content.PackStore
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val contentPlatformModule: Module = module {
    single { PackStore(context = get<Context>()) }
}
