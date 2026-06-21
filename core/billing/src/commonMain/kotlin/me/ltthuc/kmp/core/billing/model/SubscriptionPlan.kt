package me.ltthuc.kmp.core.billing.model

import kotlinx.serialization.Serializable

/**
 * Purchasable products for the per-level monetization model. All are **one-time non-consumable**
 * IAP (no subscription period).
 *
 * - `LEVEL_1`..`LEVEL_5`: unlock the paid units of a single curriculum level; each grants the
 *   matching RevenueCat entitlement `level_n`.
 * - `BUNDLE`: unlocks all five levels at once. Configured in the RevenueCat dashboard to grant all
 *   five `level_n` entitlements, so reading active entitlements yields the full owned set with no
 *   bundle-specific branching in code (`entitlementId`/`levelId` are null for the bundle itself).
 *
 * Android (Play) and iOS (App Store) use the same product id string here for non-consumables.
 */
@Serializable
enum class SubscriptionPlan(
    val androidProductId: String,
    val iosProductId: String,
    val entitlementId: String?,
    val levelId: String?,
) {
    LEVEL_1("phonics_level_1", "phonics_level_1", "level_1", "L1"),
    LEVEL_2("phonics_level_2", "phonics_level_2", "level_2", "L2"),
    LEVEL_3("phonics_level_3", "phonics_level_3", "level_3", "L3"),
    LEVEL_4("phonics_level_4", "phonics_level_4", "level_4", "L4"),
    LEVEL_5("phonics_level_5", "phonics_level_5", "level_5", "L5"),
    BUNDLE("phonics_all_levels", "phonics_all_levels", null, null),
    ;

    val isBundle: Boolean get() = this == BUNDLE

    companion object {
        /** RevenueCat entitlement id → curriculum level id (bundle excluded — it has no own entitlement). */
        val LEVEL_ENTITLEMENTS: Map<String, String> = entries
            .mapNotNull { plan -> plan.entitlementId?.let { ent -> ent to plan.levelId!! } }
            .toMap()

        /** All curriculum level ids that can be owned (granted in full by the bundle). */
        val allLevelIds: List<String> = entries.mapNotNull { it.levelId }

        /** The per-level product for [levelId], or null if none matches. */
        fun forLevel(levelId: String): SubscriptionPlan? = entries.firstOrNull { it.levelId == levelId }

        fun fromProductId(productId: String): SubscriptionPlan? =
            entries.find { it.androidProductId == productId || it.iosProductId == productId }
    }
}
