package me.ltthuc.kmp.core.common.di

import org.koin.core.module.Module

/** Platform-specific bindings (Sharer, Reviewer). */
internal expect val platformModule: Module
