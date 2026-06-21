package me.ltthuc.kmp.core.repository

import me.ltthuc.kmp.core.model.UnitStatus
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the two-gate per-unit decision ([decideUnitStatus]) for the per-level monetization model
 * (1 free unit/level, paid units gated by ownership, sequential within a level, parent override).
 */
class UnitGatingTest {

    private fun status(
        index: Int,
        isCompleted: Boolean = false,
        isActive: Boolean = false,
        prevCompleted: Boolean = false,
        developerMode: Boolean = false,
        levelOwned: Boolean = false,
        levelOpenedFully: Boolean = false,
        monetizationEnabled: Boolean = true,
    ) = decideUnitStatus(
        index = index,
        isActive = isActive,
        isCompleted = isCompleted,
        prevCompleted = prevCompleted,
        developerMode = developerMode,
        levelOwned = levelOwned,
        levelOpenedFully = levelOpenedFully,
        monetizationEnabled = monetizationEnabled,
        freeUnitsPerLevel = 1,
    )

    @Test
    fun firstUnitIsAlwaysAFreeSampleEvenWhenNotOwned() {
        assertEquals(UnitStatus.Active, status(index = 0, isActive = true, levelOwned = false))
    }

    @Test
    fun paidUnitIsPremiumLockedWhenNotOwned() {
        // index >= freeUnits, not owned, monetization on, even if the previous unit is completed.
        assertEquals(
            UnitStatus.PremiumLocked,
            status(index = 1, prevCompleted = true, levelOwned = false),
        )
    }

    @Test
    fun ownedPaidUnitFollowsSequentialGate() {
        // Owned but previous not completed → still locked sequentially (phương án A).
        assertEquals(UnitStatus.Locked, status(index = 2, prevCompleted = false, levelOwned = true))
        // Owned and previous completed → unlocks.
        assertEquals(UnitStatus.Unlocked, status(index = 2, prevCompleted = true, levelOwned = true))
    }

    @Test
    fun ownedAndOpenedFullyDropsTheSequentialGate() {
        assertEquals(
            UnitStatus.Unlocked,
            status(index = 3, prevCompleted = false, levelOwned = true, levelOpenedFully = true),
        )
    }

    @Test
    fun monetizationOffKeepsEverythingSequential() {
        // Mốc 1 behaviour: no paywall, paid index behaves sequentially.
        assertEquals(
            UnitStatus.Unlocked,
            status(index = 3, prevCompleted = true, levelOwned = false, monetizationEnabled = false),
        )
    }

    @Test
    fun developerModeForcesCompleted() {
        assertEquals(
            UnitStatus.Completed,
            status(index = 4, levelOwned = false, developerMode = true),
        )
    }
}
