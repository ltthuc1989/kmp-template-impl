package me.ltthuc.kmp.core.billing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.PurchaseResult
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.billing.model.SubscriptionState

/**
 * In-memory billing for local testing (and unit tests). Purchases succeed instantly and grant
 * ownership without any store call: a level grants that level, the bundle grants all five.
 * Selected via [USE_FAKE_BILLING] in DEBUG builds only.
 *
 * The prices below are **DEBUG-ONLY placeholders** so the paywall has something to show when there
 * is no store connection. The real, localized price comes from Play Console via RevenueCat in
 * release builds (see `RevenueCatBillingDataSource`); change the real price there, not here.
 */
class FakeBillingDataSource(
    private val levelPrice: String = "$6.99",
    private val bundlePrice: String = "$19.99",
) : BillingDataSource {
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    override val subscriptionState: Flow<SubscriptionState> = _subscriptionState.asStateFlow()

    override fun configure() = Unit

    override suspend fun getProducts(plans: List<SubscriptionPlan>): List<ProductInfo> = plans.map { plan ->
        ProductInfo(plan = plan, priceString = if (plan.isBundle) bundlePrice else levelPrice)
    }

    override suspend fun purchasePlan(plan: SubscriptionPlan): PurchaseResult {
        val granted = if (plan.isBundle) {
            SubscriptionPlan.allLevelIds.toSet()
        } else {
            getCurrentOwnedLevelIds() + listOfNotNull(plan.levelId)
        }
        _subscriptionState.value = SubscriptionState.Premium(getCurrentOwnedLevelIds() + granted)
        return PurchaseResult.Success
    }

    override suspend fun restorePurchases(): PurchaseResult = PurchaseResult.Success

    override suspend fun refresh() = Unit

    override fun getCurrentSubscriptionState(): SubscriptionState = _subscriptionState.value

    override fun getCurrentOwnedLevelIds(): Set<String> = _subscriptionState.value.ownedLevelIds
}
