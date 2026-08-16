package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import me.ltthuc.kmp.core.billing.BillingDataSource
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.PurchaseResult
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.billing.model.SubscriptionState

/**
 * Purchases, and the one place ownership changes — so it is also where a newly unlocked level
 * starts fetching its content.
 *
 * Downloading here rather than when the child opens a unit is about connectivity, not
 * convenience: buying requires a network, so this is the only moment the app *knows* it is
 * online, and the parent is holding the phone. Waiting until a unit is tapped moves the fetch to
 * an arbitrary later moment — a car, a plane, weak signal — with a 3-to-8-year-old holding the
 * device and no one able to act on a failure. Per-unit download stays as the fallback for the
 * paths that skip this one: restoring on a new device, deleting a pack, a download that failed.
 */
class BillingRepository(
    private val billingDataSource: BillingDataSource,
    private val appSettingRepository: AppSettingRepository,
    private val contentPackRepository: ContentPackRepository,
) {
    val subscriptionState: Flow<SubscriptionState> = billingDataSource.subscriptionState

    fun configure() {
        billingDataSource.configure()
    }

    /**
     * Products to show on a level-context paywall. Phase 1 ships only Level 1, so we sell just the
     * single level (no bundle — we don't advertise levels that don't exist yet). Re-add
     * [SubscriptionPlan.BUNDLE] here when L2+ content ships.
     */
    suspend fun getProductsForLevel(levelId: String?): List<ProductInfo> {
        val plan = levelId?.let { SubscriptionPlan.forLevel(it) } ?: return emptyList()
        return billingDataSource.getProducts(listOf(plan))
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
        val product = getProductsForLevel(levelId).firstOrNull() ?: return false
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
        val previouslyOwned = appSettingRepository.setting.value.ownedLevelIds
        val owned = billingDataSource.getCurrentOwnedLevelIds()

        appSettingRepository.setOwnedLevelIds(owned)
        appSettingRepository.setPlusMode(billingDataSource.getCurrentSubscriptionState().isPremium)

        // Only levels that just became owned. Deliberately not "every owned level missing
        // content": a parent who cleared a pack to free space should not have it pulled back
        // behind their back, and app launch carries no guarantee of a network.
        (owned - previouslyOwned).forEach(contentPackRepository::downloadLevelInBackground)
    }
}
