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

    @Test
    fun isBundleOnlyTrueForBundle() {
        assertTrue(SubscriptionPlan.BUNDLE.isBundle)
        assertFalse(SubscriptionPlan.LEVEL_1.isBundle)
    }
}
