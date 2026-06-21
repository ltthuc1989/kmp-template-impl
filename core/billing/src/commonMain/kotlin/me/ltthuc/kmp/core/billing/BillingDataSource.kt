package me.ltthuc.kmp.core.billing

import kotlinx.coroutines.flow.Flow
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.PurchaseResult
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.billing.model.SubscriptionState

/**
 * Billing contract. Two implementations: [RevenueCatBillingDataSource] (real store) and
 * [FakeBillingDataSource] (in-memory, for local testing + unit tests). [BillingRepository] depends
 * only on this interface, so production code has no test-only branches.
 */
interface BillingDataSource {
    val subscriptionState: Flow<SubscriptionState>

    fun configure()

    /** Localized product info for [plans] (those without a matching store package are skipped). */
    suspend fun getProducts(plans: List<SubscriptionPlan>): List<ProductInfo>

    /** Launch the purchase flow for [plan]. */
    suspend fun purchasePlan(plan: SubscriptionPlan): PurchaseResult

    suspend fun restorePurchases(): PurchaseResult

    /** Re-fetch ownership from the store and refresh [subscriptionState]. */
    suspend fun refresh()

    fun getCurrentSubscriptionState(): SubscriptionState

    /** Curriculum levels currently owned (derived from active entitlements / fake state). */
    fun getCurrentOwnedLevelIds(): Set<String>
}
