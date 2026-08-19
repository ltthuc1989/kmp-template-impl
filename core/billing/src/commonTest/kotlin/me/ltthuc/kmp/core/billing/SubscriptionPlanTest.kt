package me.ltthuc.kmp.core.billing

import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SubscriptionPlanTest {

    @Test
    fun entitlementMapCoversAllFiveLevelsAndExcludesBundle() {
        assertEquals(
            mapOf(
                "level_1" to "L1",
                "level_2" to "L2",
                "level_3" to "L3",
                "level_4" to "L4",
                "level_5" to "L5",
            ),
            SubscriptionPlan.LEVEL_ENTITLEMENTS,
        )
    }

    @Test
    fun allLevelIdsHasFiveLevelsWithoutBundle() {
        assertEquals(listOf("L1", "L2", "L3", "L4", "L5"), SubscriptionPlan.allLevelIds)
    }

    @Test
    fun forLevelResolvesTheMatchingProduct() {
        assertEquals(SubscriptionPlan.LEVEL_3, SubscriptionPlan.forLevel("L3"))
        assertNull(SubscriptionPlan.forLevel("L9"))
    }

    @Test
    fun fromProductIdResolvesLevelAndBundle() {
        assertEquals(SubscriptionPlan.LEVEL_4, SubscriptionPlan.fromProductId("phonics_level_4"))
        assertEquals(SubscriptionPlan.BUNDLE, SubscriptionPlan.fromProductId("phonics_all_levels"))
        assertNull(SubscriptionPlan.fromProductId("unknown"))
    }

    /**
     * Pins every id that has to match a dashboard entry. These four strings per plan are spread
     * across Play Console, RevenueCat products, RevenueCat entitlements and curriculum.json — a
     * typo in any of them fails silently at runtime (empty product list, or a purchase that never
     * unlocks). Fail here instead.
     */
    @Test
    fun everyPlanPinsItsStoreIds() {
        val expected = listOf(
            //          android + ios productId, entitlementId, levelId
            SubscriptionPlan.LEVEL_1 to listOf("phonics_level_1", "level_1", "L1"),
            SubscriptionPlan.LEVEL_2 to listOf("phonics_level_2", "level_2", "L2"),
            SubscriptionPlan.LEVEL_3 to listOf("phonics_level_3", "level_3", "L3"),
            SubscriptionPlan.LEVEL_4 to listOf("phonics_level_4", "level_4", "L4"),
            SubscriptionPlan.LEVEL_5 to listOf("phonics_level_5", "level_5", "L5"),
        )

        for ((plan, ids) in expected) {
            val (productId, entitlementId, levelId) = ids
            assertEquals(productId, plan.androidProductId, "androidProductId of $plan")
            assertEquals(productId, plan.iosProductId, "iosProductId of $plan")
            assertEquals(entitlementId, plan.entitlementId, "entitlementId of $plan")
            assertEquals(levelId, plan.levelId, "levelId of $plan")

            // The two lookups the paywall actually goes through.
            assertEquals(plan, SubscriptionPlan.forLevel(levelId), "forLevel($levelId)")
            assertEquals(plan, SubscriptionPlan.fromProductId(productId), "fromProductId($productId)")
        }

        assertEquals("phonics_all_levels", SubscriptionPlan.BUNDLE.androidProductId)
    }

    @Test
    fun isBundleOnlyTrueForBundle() {
        assertTrue(SubscriptionPlan.BUNDLE.isBundle)
        assertFalse(SubscriptionPlan.LEVEL_1.isBundle)
    }
}
