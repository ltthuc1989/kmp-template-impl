package me.ltthuc.kmp.core.repository

import me.ltthuc.kmp.core.model.AppSetting
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the pure access rule ([grantsLevelAccess]) that every gate — unit locking, paywall,
 * content-pack downloads — routes through. The point of these tests is that a level opened
 * by a non-purchase source is indistinguishable from a purchased one to every caller.
 */
class LevelAccessTest {

    private fun setting(
        owned: Set<String> = emptySet(),
        adUnlocked: Set<String> = emptySet(),
        manualUnlocked: Set<String> = emptySet(),
        developerMode: Boolean = false,
        plusMode: Boolean = false,
    ) = AppSetting.DEFAULT.copy(
        ownedLevelIds = owned,
        adUnlockedLevelIds = adUnlocked,
        manualUnlockedLevelIds = manualUnlocked,
        developerMode = developerMode,
        plusMode = plusMode,
    )

    @Test
    fun purchasedLevelIsOpen() {
        assertTrue(grantsLevelAccess(setting(owned = setOf("L2")), "L2"))
    }

    @Test
    fun unpurchasedLevelIsClosed() {
        assertFalse(grantsLevelAccess(setting(owned = setOf("L2")), "L3"))
    }

    @Test
    fun adUnlockOpensLevelWithoutPurchase() {
        assertTrue(grantsLevelAccess(setting(adUnlocked = setOf("L3")), "L3"))
    }

    @Test
    fun monetizationOffOpensEverything() {
        assertTrue(grantsLevelAccess(setting(), "L5", monetizationEnabled = false))
    }

    @Test
    fun developerModeOpensEverything() {
        assertTrue(grantsLevelAccess(setting(developerMode = true), "L5"))
    }

    @Test
    fun plusModeAloneDoesNotOpenALevel() {
        // Owning one level must not unlock the others.
        assertFalse(grantsLevelAccess(setting(plusMode = true), "L2"))
    }

    @Test
    fun manualUnlockNeverBypassesTheGate() {
        assertFalse(opensLevelFully(setting(manualUnlocked = setOf("L2")), "L2"))
    }

    @Test
    fun manualUnlockAppliesOnceTheLevelIsOpen() {
        val s = setting(owned = setOf("L2"), manualUnlocked = setOf("L2"))
        assertTrue(opensLevelFully(s, "L2"))
    }

    @Test
    fun manualUnlockAppliesToAnAdUnlockedLevelToo() {
        val s = setting(adUnlocked = setOf("L2"), manualUnlocked = setOf("L2"))
        assertTrue(opensLevelFully(s, "L2"))
    }
}
