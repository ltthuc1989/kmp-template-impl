package me.ltthuc.kmp

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import me.ltthuc.kmp.di.applyModules
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        applyModules()
    }
}

fun initNapier() {
    Napier.base(DebugAntilog())
}
