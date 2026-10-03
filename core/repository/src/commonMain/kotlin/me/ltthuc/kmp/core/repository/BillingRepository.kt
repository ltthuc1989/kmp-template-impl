package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import me.ltthuc.kmp.core.billing.BillingDataSource
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.PurchaseResult
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.billing.model.SubscriptionState

class BillingRepository(
    private val billingDataSource: BillingDataSource,
    private val appSettingRepository: AppSettingRepository,
) {
    val subscriptionState: Flow<SubscriptionState> = billingDataSource.subscriptionState

    fun configure() {
        billingDataSource.configure()
    }

    /**
     * What the paywall for [levelId] sells: that level, then the all-levels bundle.
     *
     * The bundle was held back while only two levels had shipped — it would have charged for three
     * that did not exist yet. With all five launched (2026-10-03) it goes back on sale. Level comes
     * FIRST: the paywall pre-selects the first product, and a parent who opened one level's paywall
     * should not land on the bigger purchase by default. A store without the bundle product just
     * returns the level alone.
     */
    suspend fun getProductsForLevel(levelId: String?): List<ProductInfo> {
        val plan = levelId?.let { SubscriptionPlan.forLevel(it) } ?: return emptyList()
        return billingDataSource.getProducts(listOf(plan, SubscriptionPlan.BUNDLE))
            .sortedBy { it.plan.isBundle }
    }

    suspend fun getProducts(): List<ProductInfo> =
        billingDataSource.getProducts(SubscriptionPlan.entries)

    /**
     * Localized unlock price per level (levelId → priceString) for UI that shows a price without
     * touching billing types. Empty for levels the store has no product for. Real price comes from
     * the store; in debug/fake billing it's a placeholder.
     */
    suspend fun getLevelPrices(): Map<String, String> =
        getProducts().mapNotNull { product -> product.plan.levelId?.let { it to product.priceString } }.toMap()

    suspend fun purchase(productInfo: ProductInfo): PurchaseResult {
        val result = billingDataSource.purchasePlan(productInfo.plan)
        if (result == PurchaseResult.Success) syncOwnedLevels()
        return result
    }

    /**
     * Buy a level directly (one product, no paywall) and return true once it's owned. Used by the
     * Settings "Unlock" entry where the parent already decided — hides billing types from callers.
     */
    suspend fun purchaseLevel(levelId: String): Boolean {
        // Exactly this level's product — never the bundle, even when the level product is missing
        // from the store and the bundle is the only thing [getProductsForLevel] returned.
        val plan = SubscriptionPlan.forLevel(levelId) ?: return false
        val product = getProductsForLevel(levelId).firstOrNull { it.plan == plan } ?: return false
        return purchase(product) == PurchaseResult.Success && isLevelOwned(levelId)
    }

    suspend fun restorePurchases(): PurchaseResult {
        val result = billingDataSource.restorePurchases()
        if (result == PurchaseResult.Success) syncOwnedLevels()
        return result
    }

    /** Restore + sync; returns true on success. Hides the billing-specific result type from callers. */
    suspend fun restore(): Boolean = restorePurchases() == PurchaseResult.Success

    fun isPremium(): Boolean = billingDataSource.getCurrentSubscriptionState().isPremium

    fun ownedLevelIds(): Set<String> = billingDataSource.getCurrentOwnedLevelIds()

    fun isLevelOwned(levelId: String): Boolean = levelId in ownedLevelIds()

    /** Re-reads ownership from the store and syncs it into AppSetting (offline source of truth). */
    suspend fun verifySubscriptionStatus() {
        billingDataSource.refresh()
        syncOwnedLevels()
    }

    private suspend fun syncOwnedLevels() {
        appSettingRepository.setOwnedLevelIds(billingDataSource.getCurrentOwnedLevelIds())
        appSettingRepository.setPlusMode(billingDataSource.getCurrentSubscriptionState().isPremium)
    }
}
