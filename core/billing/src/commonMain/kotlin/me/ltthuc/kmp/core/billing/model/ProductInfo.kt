package me.ltthuc.kmp.core.billing.model

import androidx.compose.runtime.Stable

/**
 * A purchasable product as shown on the paywall. Decoupled from RevenueCat types so the fake
 * billing source (and unit tests) can build it without a real `StoreProduct`/`Package`.
 *
 * @param plan which product this is (a level or the bundle)
 * @param priceString already-localized price (e.g. "$4.99"); from the store in real mode, hardcoded in fake mode
 */
@Stable
data class ProductInfo(
    val plan: SubscriptionPlan,
    val priceString: String,
) {
    val isBundle: Boolean get() = plan.isBundle
}
