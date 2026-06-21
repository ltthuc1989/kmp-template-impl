package me.ltthuc.kmp.core.billing

import kotlinx.coroutines.test.runTest
import me.ltthuc.kmp.core.billing.model.PurchaseResult
import me.ltthuc.kmp.core.billing.model.SubscriptionPlan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeBillingDataSourceTest {

    @Test
    fun buyingASingleLevelGrantsOnlyThatLevel() = runTest {
        val ds = FakeBillingDataSource()

        val result = ds.purchasePlan(SubscriptionPlan.LEVEL_2)

        assertEquals(PurchaseResult.Success, result)
        assertEquals(setOf("L2"), ds.getCurrentOwnedLevelIds())
        assertTrue(ds.getCurrentSubscriptionState().isPremium)
    }

    @Test
    fun buyingTwoLevelsAccumulatesOwnership() = runTest {
        val ds = FakeBillingDataSource()

        ds.purchasePlan(SubscriptionPlan.LEVEL_1)
        ds.purchasePlan(SubscriptionPlan.LEVEL_3)

        assertEquals(setOf("L1", "L3"), ds.getCurrentOwnedLevelIds())
    }

    @Test
    fun buyingTheBundleGrantsAllFiveLevels() = runTest {
        val ds = FakeBillingDataSource()

        ds.purchasePlan(SubscriptionPlan.BUNDLE)

        assertEquals(setOf("L1", "L2", "L3", "L4", "L5"), ds.getCurrentOwnedLevelIds())
    }

    @Test
    fun startsWithNothingOwned() = runTest {
        val ds = FakeBillingDataSource()

        assertFalse(ds.getCurrentSubscriptionState().isPremium)
        assertTrue(ds.getCurrentOwnedLevelIds().isEmpty())
    }

    @Test
    fun getProductsReturnsLevelAndBundlePrices() = runTest {
        val ds = FakeBillingDataSource(levelPrice = "$4.99", bundlePrice = "$19.99")

        val products = ds.getProducts(listOf(SubscriptionPlan.LEVEL_2, SubscriptionPlan.BUNDLE))

        assertEquals("$4.99", products.first { !it.isBundle }.priceString)
        assertEquals("$19.99", products.first { it.isBundle }.priceString)
    }
}
