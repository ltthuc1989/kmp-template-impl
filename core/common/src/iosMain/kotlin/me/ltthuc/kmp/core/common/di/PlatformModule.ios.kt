package me.ltthuc.kmp.core.common.di

import me.ltthuc.kmp.core.common.share.Reviewer
import me.ltthuc.kmp.core.common.share.Sharer
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single { Sharer() }
    single { Reviewer() }
}
