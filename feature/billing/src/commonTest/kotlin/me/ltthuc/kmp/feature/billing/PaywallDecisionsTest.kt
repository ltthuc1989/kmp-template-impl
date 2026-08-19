package me.ltthuc.kmp.feature.billing

import me.ltthuc.kmp.core.billing.model.PurchaseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The paywall sells one level, but ownership used to be read with the global "owns anything"
 * check. A parent who owned Level 1 and tapped Restore on the Level 2 paywall saw it close as if
 * Level 2 had been unlocked. These tests pin the per-level rule so that cannot come back.
 */
class PaywallDecisionsTest {

    // region ownsPaywallTarget

    @Test
    fun ownsTargetOnlyWhenThatExactLevelIsOwned() {
        assertTrue(ownsPaywallTarget("L2", setOf("L2"), isPremium = true))
    }

    @Test
    fun owningAnotherLevelDoesNotCountAsOwningThisOne() {
        // The regression: premium is true because Level 1 is owned, but this paywall sells Level 2.
        assertFalse(ownsPaywallTarget("L2", setOf("L1"), isPremium = true))
    }

    @Test
    fun bundleGrantsEveryLevelSoTargetIsOwned() {
        assertTrue(ownsPaywallTarget("L4", setOf("L1", "L2", "L3", "L4", "L5"), isPremium = true))
    }

    @Test
    fun withNoLevelInMindItFallsBackToTheGlobalCheck() {
        assertTrue(ownsPaywallTarget(null, emptySet(), isPremium = true))
        assertFalse(ownsPaywallTarget(null, emptySet(), isPremium = false))
    }

    // endregion

    // region purchaseOutcome

    @Test
    fun purchaseSucceedsOnlyWhenTheLevelActuallyLanded() {
        assertEquals(PurchaseUiState.Success, purchaseOutcome(PurchaseResult.Success, ownsTarget = true))
    }

    @Test
    fun purchaseThatChargedButGrantedNothingIsReportedAsFailed() {
        // Store said Success, entitlement never arrived. Reporting Success here would close the
        // paywall on a level that is still locked.
        assertEquals(PurchaseUiState.PurchaseFailed, purchaseOutcome(PurchaseResult.Success, ownsTarget = false))
    }

    @Test
    fun cancellingAPurchaseLeavesTheScreenAlone() {
        assertEquals(PurchaseUiState.Idle, purchaseOutcome(PurchaseResult.Cancelled, ownsTarget = false))
    }

    @Test
    fun purchaseErrorCarriesItsMessage() {
        assertEquals(
            PurchaseUiState.Error("boom"),
            purchaseOutcome(PurchaseResult.Error("boom"), ownsTarget = false),
        )
    }

    // endregion

    // region restoreOutcome

    @Test
    fun restoreSucceedsWhenItBroughtThisLevelBack() {
        assertEquals(PurchaseUiState.Success, restoreOutcome(PurchaseResult.Success, ownsTarget = true))
    }

    @Test
    fun restoreWithNothingForThisLevelStaysOnThePaywall() {
        // RevenueCat reports Success even with nothing to restore — the reason Restore used to pop
        // the screen for anyone who owned any other level.
        assertEquals(
            PurchaseUiState.NoSubscriptionToRestore,
            restoreOutcome(PurchaseResult.Success, ownsTarget = false),
        )
    }

    @Test
    fun restoreErrorCarriesItsMessage() {
        assertEquals(
            PurchaseUiState.Error("network"),
            restoreOutcome(PurchaseResult.Error("network"), ownsTarget = false),
        )
    }

    // endregion
}
