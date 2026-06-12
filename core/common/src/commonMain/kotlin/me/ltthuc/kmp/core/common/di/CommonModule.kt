package me.ltthuc.kmp.core.common.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import me.ltthuc.kmp.core.common.formatter
import org.koin.dsl.module

val commonModule = module {
    single { formatter }
    single<CoroutineDispatcher> { Dispatchers.IO }

    includes(platformModule)
}
