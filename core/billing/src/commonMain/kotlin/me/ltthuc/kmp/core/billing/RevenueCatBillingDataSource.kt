package me.ltthuc.kmp.core.billing

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Offerings
import com.revenuecat.purchases.kmp.models.Package
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.PurchaseResult
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.billing.model.SubscriptionState
import me.ltthuc.kmp.core.model.AppConfig
import me.ltthuc.kmp.core.model.Platform
import me.ltthuc.kmp.core.model.currentPlatform
import kotlin.coroutines.resume

/**
 * Real billing backed by RevenueCat. Ownership is derived from active entitlements (`level_n`),
 * mapped to curriculum level ids via [SubscriptionPlan.LEVEL_ENTITLEMENTS].
 */
class RevenueCatBillingDataSource(
    private val appConfig: AppConfig,
) : BillingDataSource {
    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)

    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Loading)
    override val subscriptionState: Flow<SubscriptionState> = _subscriptionState.asStateFlow()

    private var isConfigured = false

    override fun configure() {
        if (isConfigured) return

        val apiKey = when (currentPlatform) {
            Platform.Android -> appConfig.purchaseAndroidApiKey
            Platform.IOS -> appConfig.purchaseIosApiKey
        }

        if (apiKey.isNullOrBlank()) {
            _subscriptionState.value = SubscriptionState.Free
            return
        }

        Purchases.configure(configuration = PurchasesConfiguration(apiKey = apiKey))
        isConfigured = true

        Purchases.sharedInstance.getCustomerInfo(
            onSuccess = { info -> updateFrom(info) },
            onError = {
                Napier.e("Failed to fetch customer info: ${it.message}")
                _subscriptionState.value = SubscriptionState.Free
            },
        )
    }

    override suspend fun getProducts(plans: List<SubscriptionPlan>): List<ProductInfo> {
        val packages = currentPackages() ?: return emptyList()
        return plans.mapNotNull { plan ->
            val pkg = packages.findByPlan(plan) ?: return@mapNotNull null
            ProductInfo(plan = plan, priceString = pkg.storeProduct.price.formatted)
        }
    }

    override suspend fun purchasePlan(plan: SubscriptionPlan): PurchaseResult {
        if (!isConfigured) return PurchaseResult.Error("Billing not configured")
        val pkg = currentPackages()?.findByPlan(plan)
            ?: return PurchaseResult.Error("Product not available: ${plan.androidProductId}")

        return suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.purchase(
                packageToPurchase = pkg,
                onSuccess = { _, info ->
                    updateFrom(info)
                    cont.resume(PurchaseResult.Success)
                },
                onError = { error, userCancelled ->
                    Napier.e("Failed to purchase: ${error.message}")
                    cont.resume(
                        if (userCancelled) PurchaseResult.Cancelled else PurchaseResult.Error(error.message),
                    )
                },
            )
        }
    }

    override suspend fun restorePurchases(): PurchaseResult {
        if (!isConfigured) return PurchaseResult.Error("Billing not configured")
        return suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchases(
                onSuccess = { info ->
                    updateFrom(info)
                    cont.resume(PurchaseResult.Success)
                },
                onError = { error ->
                    Napier.e("Failed to restore: ${error.message}")
                    cont.resume(PurchaseResult.Error(error.message))
                },
            )
        }
    }

    override suspend fun refresh() {
        if (!isConfigured) return
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getCustomerInfo(
                onSuccess = { info ->
                    updateFrom(info)
                    cont.resume(Unit)
                },
                onError = {
                    Napier.e("Failed to refresh customer info: ${it.message}")
                    cont.resume(Unit)
                },
            )
        }
    }

    override fun getCurrentSubscriptionState(): SubscriptionState = _subscriptionState.value

    override fun getCurrentOwnedLevelIds(): Set<String> = _subscriptionState.value.ownedLevelIds

    private suspend fun currentPackages(): List<Package>? {
        if (!isConfigured) return null
        val offerings = getOfferings() ?: return null
        return offerings.current?.availablePackages
    }

    private suspend fun getOfferings(): Offerings? = suspendCancellableCoroutine { cont ->
        Purchases.sharedInstance.getOfferings(
            onSuccess = { cont.resume(it) },
            onError = {
                Napier.e("Failed to fetch offerings: ${it.message}")
                cont.resume(null)
            },
        )
    }

    private fun List<Package>.findByPlan(plan: SubscriptionPlan): Package? = firstOrNull {
        it.storeProduct.id == plan.androidProductId || it.storeProduct.id == plan.iosProductId
    }

    private fun updateFrom(info: CustomerInfo) {
        _customerInfo.value = info
        val owned = info.entitlements.active.keys
            .mapNotNull { SubscriptionPlan.LEVEL_ENTITLEMENTS[it] }
            .toSet()
        _subscriptionState.value =
            if (owned.isEmpty()) SubscriptionState.Free else SubscriptionState.Premium(owned)
    }
}
