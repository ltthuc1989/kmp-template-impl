package me.ltthuc.kmp.core.billing.di

import me.ltthuc.kmp.core.billing.BillingDataSource
import me.ltthuc.kmp.core.billing.FakeBillingDataSource
import me.ltthuc.kmp.core.billing.RevenueCatBillingDataSource
import me.ltthuc.kmp.core.billing.USE_FAKE_BILLING
import org.koin.dsl.module

val billingModule = module {
    single<BillingDataSource> {
        if (USE_FAKE_BILLING) FakeBillingDataSource() else RevenueCatBillingDataSource(get())
    }
}
