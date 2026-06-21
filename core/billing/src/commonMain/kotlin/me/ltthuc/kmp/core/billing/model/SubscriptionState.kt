package me.ltthuc.kmp.core.billing.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

/**
 * Billing/ownership state. Per-level model uses one-time purchases, so there is no expiry/renewal —
 * `Premium` simply carries the set of owned level ids (derived from active RevenueCat entitlements).
 */
@Stable
@Serializable
sealed interface SubscriptionState {
    /** Loading customer info. */
    @Serializable
    data object Loading : SubscriptionState

    /** No active purchase. */
    @Serializable
    data object Free : SubscriptionState

    /** Owns at least one level (or the bundle → all five). */
    @Serializable
    data class Premium(
        override val ownedLevelIds: Set<String>,
    ) : SubscriptionState

    val isPremium: Boolean
        get() = this is Premium

    val ownedLevelIds: Set<String>
        get() = emptySet()
}
