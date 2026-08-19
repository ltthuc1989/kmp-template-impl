package me.ltthuc.kmp.feature.billing

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.billing.model.ProductInfo
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import me.ltthuc.kmp.core.common.suspendRunCatching
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.repository.BillingRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.error_billing
import me.ltthuc.kmp.core.resource.error_network
import me.ltthuc.kmp.core.ui.screen.ScreenState

class PaywallViewModel(
    private val billingRepository: BillingRepository,
    private val levelRepository: LevelRepository,
    private val levelId: String?,
) : ViewModel() {

    /** The level this paywall is actually selling, so the copy can name it. */
    val level: StateFlow<Level?> = levelRepository.observeLevelCards()
        .map { cards -> cards.firstOrNull { it.level.id == levelId }?.level }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _screenState = MutableStateFlow<ScreenState<PaywallUiState>>(ScreenState.Loading())
    val screenState: StateFlow<ScreenState<PaywallUiState>> = _screenState.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseUiState>(PurchaseUiState.Idle)
    val purchaseState: StateFlow<PurchaseUiState> = _purchaseState.asStateFlow()

    // Defaults to the level product; updated to the actual fetched product in [fetch].
    private val _selectedPlan =
        MutableStateFlow(SubscriptionPlan.forLevel(levelId.orEmpty()) ?: SubscriptionPlan.LEVEL_1)
    val selectedPlan: StateFlow<SubscriptionPlan> = _selectedPlan.asStateFlow()

    private var products: List<ProductInfo> = emptyList()

    init {
        fetch()
    }

    fun fetch() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading()
            _screenState.value = suspendRunCatching {
                val fetched = billingRepository.getProductsForLevel(levelId)
                products = fetched
                fetched.firstOrNull()?.let { _selectedPlan.value = it.plan }
                PaywallUiState(products = fetched.toImmutableList())
            }.fold(
                onSuccess = {
                    // An empty list is not a success: the store returned no package for this level
                    // (product missing from the current RevenueCat offering, inactive in Play, or
                    // billing never configured). Showing the paywall anyway leaves an empty plan
                    // selector and a Buy button that does nothing, so surface it with a retry.
                    if (it.products.isEmpty()) {
                        ScreenState.Error(Res.string.error_billing)
                    } else {
                        ScreenState.Idle(it)
                    }
                },
                onFailure = { ScreenState.Error(Res.string.error_network) },
            )
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    /** Ownership scoped to what this paywall actually sells. Rule lives in [ownsPaywallTarget]. */
    private fun ownsThisLevel(): Boolean =
        ownsPaywallTarget(levelId, billingRepository.ownedLevelIds(), billingRepository.isPremium())

    fun purchase() {
        val product = products.find { it.plan == _selectedPlan.value }
        if (product == null) {
            // Nothing to buy for the selected plan. Returning silently here was the reason a failed
            // purchase showed no dialog and no message at all.
            _purchaseState.value = PurchaseUiState.PurchaseFailed
            return
        }

        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState.Loading
            // Ownership is read *after* the call, so it reflects the entitlement the store just
            // granted (or failed to).
            val result = billingRepository.purchase(product)
            _purchaseState.value = purchaseOutcome(result, ownsThisLevel())
        }
    }

    fun restore() {
        viewModelScope.launch {
            _purchaseState.value = PurchaseUiState.Loading
            val result = billingRepository.restorePurchases()
            _purchaseState.value = restoreOutcome(result, ownsThisLevel())
        }
    }
}

@Stable
data class PaywallUiState(
    val products: ImmutableList<ProductInfo>,
)

@Stable
sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Loading : PurchaseUiState
    data object Success : PurchaseUiState
    data object PurchaseFailed : PurchaseUiState
    data object NoSubscriptionToRestore : PurchaseUiState
    data class Error(val message: String) : PurchaseUiState
}
