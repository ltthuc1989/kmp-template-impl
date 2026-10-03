package me.ltthuc.kmp.feature.billing

import me.ltthuc.kmp.core.billing.model.PurchaseResult

/**
 * Pure decision rules behind the paywall, kept top-level so they can be tested without a
 * ViewModel, a repository or a DataStore — the same reason `grantsLevelAccess` lives outside
 * `LevelAccess`.
 */

/**
 * Does the buyer already own what *this* paywall sells?
 *
 * [isPremium] is global — true as soon as any level is owned — so a parent who owns Level 1 and
 * opens the Level 2 paywall must not be told they own Level 2. Falls back to [isPremium] only
 * when the paywall is not selling one specific level.
 */
/** "L3" → 3. Level ids are `L<n>` (see `SubscriptionPlan.levelId`); null for anything else. */
internal fun levelNumberOf(levelId: String): Int? = levelId.removePrefix("L").toIntOrNull()

internal fun ownsPaywallTarget(
    levelId: String?,
    ownedLevelIds: Set<String>,
    isPremium: Boolean,
): Boolean = if (levelId != null) levelId in ownedLevelIds else isPremium

/**
 * What the screen shows after a purchase attempt.
 *
 * A store `Success` is not proof of anything on its own: the transaction can go through while the
 * entitlement fails to land. Only [ownsTarget] decides.
 */
internal fun purchaseOutcome(result: PurchaseResult, ownsTarget: Boolean): PurchaseUiState = when (result) {
    PurchaseResult.Success ->
        if (ownsTarget) PurchaseUiState.Success else PurchaseUiState.PurchaseFailed

    PurchaseResult.Cancelled -> PurchaseUiState.Idle
    is PurchaseResult.Error -> PurchaseUiState.Error(result.message)
}

/**
 * What the screen shows after a restore attempt.
 *
 * RevenueCat reports `Success` even when there was nothing to restore, so the result alone would
 * close the paywall on a buyer who owns nothing for this level.
 *
 * Restore brings back *every* level on the store account, not just this one. When this level is
 * still unowned but others came back, say so — "nothing to restore" would be false, since those
 * levels did just unlock.
 */
internal fun restoreOutcome(
    result: PurchaseResult,
    ownsTarget: Boolean,
    targetLevelId: String? = null,
    ownedLevelIds: Set<String> = emptySet(),
): PurchaseUiState = when (result) {
    PurchaseResult.Success -> {
        val others = ownedLevelIds - setOfNotNull(targetLevelId)
        when {
            ownsTarget -> PurchaseUiState.Success
            targetLevelId != null && others.isNotEmpty() ->
                PurchaseUiState.RestoredOtherLevels(
                    missingLevelId = targetLevelId,
                    restoredLevelIds = others.sortedBy { levelNumberOf(it) ?: Int.MAX_VALUE },
                )
            else -> PurchaseUiState.NoSubscriptionToRestore
        }
    }

    PurchaseResult.Cancelled -> PurchaseUiState.Idle
    is PurchaseResult.Error -> PurchaseUiState.Error(result.message)
}
