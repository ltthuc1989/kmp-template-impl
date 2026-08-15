package me.ltthuc.kmp.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.ltthuc.kmp.core.model.AppSetting
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED

/**
 * The single answer to "may this user open the paid units of a level?".
 *
 * Access can come from several sources — a purchase, a parent-gated rewarded ad, a promo,
 * developer mode, or monetization being off entirely — and which sources exist is a
 * business decision that changes over time. Everything that gates on access (unit locking,
 * the paywall, and the content-pack downloader) asks here, so adding or removing a source
 * is one edit in this file instead of one edit per caller. Callers must not read
 * [AppSetting.ownedLevelIds] directly.
 */
class LevelAccess(
    private val appSettingRepository: AppSettingRepository,
) {
    /** Reactive form, for UI and long-lived observers. */
    fun canOpenPaidUnits(levelId: String): Flow<Boolean> =
        appSettingRepository.setting
            .map { grantsLevelAccess(it, levelId) }
            .distinctUntilChanged()

    /** Snapshot form for one-shot decisions (e.g. "should this download start now?"). */
    fun canOpenPaidUnitsNow(levelId: String): Boolean =
        grantsLevelAccess(appSettingRepository.setting.value, levelId)

    /**
     * True when every unit of [levelId] opens at once — the parent removed the sequential
     * gate. Only applies to a level the user may already open, so it is never a way past
     * the paywall.
     */
    fun isOpenedFully(levelId: String): Flow<Boolean> =
        appSettingRepository.setting
            .map { opensLevelFully(it, levelId) }
            .distinctUntilChanged()

    /**
     * Same rules, evaluated against a snapshot the caller already collected. Use these
     * inside an existing `combine` over [AppSettingRepository.setting] so the decision
     * comes from the emitted value instead of re-reading the flow.
     */
    fun canOpenPaidUnits(setting: AppSetting, levelId: String): Boolean =
        grantsLevelAccess(setting, levelId)

    fun isOpenedFully(setting: AppSetting, levelId: String): Boolean =
        opensLevelFully(setting, levelId)
}

/**
 * Pure access rule, kept top-level so it can be tested without a DataStore.
 *
 * Deliberately NOT consulted: `plusMode`. Owning one level must not unlock the others.
 */
internal fun grantsLevelAccess(
    setting: AppSetting,
    levelId: String,
    monetizationEnabled: Boolean = MONETIZATION_ENABLED,
): Boolean = when {
    // Mốc 1 shape: no paywall at all, everything open.
    !monetizationEnabled -> true
    setting.developerMode -> true
    levelId in setting.ownedLevelIds -> true
    // Nothing writes this set yet — see AppSetting.adUnlockedLevelIds.
    levelId in setting.adUnlockedLevelIds -> true
    else -> false
}

internal fun opensLevelFully(
    setting: AppSetting,
    levelId: String,
    monetizationEnabled: Boolean = MONETIZATION_ENABLED,
): Boolean = setting.developerMode ||
    (grantsLevelAccess(setting, levelId, monetizationEnabled) && levelId in setting.manualUnlockedLevelIds)
