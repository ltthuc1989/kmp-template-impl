package me.ltthuc.kmp.feature.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.common.share.Reviewer
import me.ltthuc.kmp.core.common.share.Sharer
import me.ltthuc.kmp.core.common.share.StoreLinks
import me.ltthuc.kmp.core.model.AppThemePalette
import me.ltthuc.kmp.core.model.Language
import me.ltthuc.kmp.core.model.Level
import me.ltthuc.kmp.core.model.LevelStatus
import me.ltthuc.kmp.core.model.Theme
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.BillingRepository
import me.ltthuc.kmp.core.repository.ContentPackRepository
import me.ltthuc.kmp.core.repository.LevelRepository
import me.ltthuc.kmp.core.repository.ProgressResetRepository

class SettingViewModel(
    private val repository: AppSettingRepository,
    private val sharer: Sharer,
    private val reviewer: Reviewer,
    private val progressResetRepository: ProgressResetRepository,
    private val levelRepository: LevelRepository,
    private val billingRepository: BillingRepository,
    private val contentPackRepository: ContentPackRepository,
) : ViewModel() {
    val setting = repository.setting

    /**
     * The levels a parent can actually buy here, in curriculum order.
     *
     * Filtered by the same [LevelStatus.ComingSoon] the Home screen renders, so a level appears
     * in this section on the build that ships its content and not before — the shipped set lives
     * in one place (`LevelRepository.LAUNCHED_PREMIUM_LEVELS`). An earlier hardcoded "level 1 only"
     * filter here meant shipping Level 2 left its unlock entry missing from Settings.
     */
    val levels: StateFlow<List<Level>> = levelRepository.observeLevelCards()
        .map { cards -> cards.filterNot { it.status is LevelStatus.ComingSoon }.map { it.level } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Real localized unlock price per level (levelId → priceString) from the store / fake billing. */
    private val _levelPrices = MutableStateFlow<Map<String, String>>(emptyMap())
    val levelPrices: StateFlow<Map<String, String>> = _levelPrices.asStateFlow()

    init {
        viewModelScope.launch {
            _levelPrices.value = runCatching { billingRepository.getLevelPrices() }
                .getOrDefault(emptyMap())
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            repository.setTheme(theme)
        }
    }

    fun setLanguage(language: Language) {
        viewModelScope.launch {
            repository.setLanguage(language)
        }
    }

    fun setAppThemePalette(palette: AppThemePalette) {
        viewModelScope.launch {
            repository.setAppThemePalette(palette)
        }
    }

    fun setDeveloperMode(developerMode: Boolean) {
        viewModelScope.launch {
            repository.setDeveloperMode(developerMode)
        }
    }

    fun setShowSpeakButton(value: Boolean) {
        viewModelScope.launch {
            repository.setShowSpeakButton(value)
        }
    }

    fun setSfxEnabled(value: Boolean) {
        viewModelScope.launch { repository.setSfxEnabled(value) }
    }

    fun setVoiceEnabled(value: Boolean) {
        viewModelScope.launch { repository.setVoiceEnabled(value) }
    }

    fun setMusicEnabled(value: Boolean) {
        viewModelScope.launch { repository.setMusicEnabled(value) }
    }

    /** Opens the native share sheet with the localized [invite] plus the store link. */
    fun shareApp(invite: String) {
        sharer.shareText("$invite\n${StoreLinks.PLAY_STORE_URL}")
    }

    /** Triggers the platform in-app review prompt (falls back to the store listing). */
    fun rateApp() {
        reviewer.requestReview()
    }

    /** Buys [levelId] directly (no paywall) — opens the store payment sheet; ownership syncs reactively. */
    fun purchaseLevel(levelId: String) {
        viewModelScope.launch { billingRepository.purchaseLevel(levelId) }
    }

    /** Restores previously purchased levels from the store (account-based; no re-charge). */
    fun restorePurchases() {
        viewModelScope.launch { billingRepository.restore() }
    }

    /** Wipes all local learning progress, then invokes [onDone] on completion. */
    /**
     * QA: forget every purchase so the paywall can be walked again. Developer mode opens all
     * levels outright, which makes the gate impossible to see — so the way to exercise the real
     * flow is to reset here, switch developer mode off, and buy through the (fake) paywall.
     */
    fun resetPurchases() {
        viewModelScope.launch { repository.clearOwnedLevels() }
    }

    /**
     * QA: mark every level as bought, without developer mode.
     *
     * Developer mode is too blunt to test with — it also reports every unit as Completed, so
     * the sequential gate, the download badge and the progress bar all stop behaving like they
     * will for a real user. Writing the owned set instead leaves every rule running and only
     * changes the one thing a purchase would change.
     */
    fun unlockAllLevelsAsPurchased() {
        viewModelScope.launch {
            repository.setOwnedLevelIds(levels.value.map { it.id }.toSet())
            // No download here: a real purchase only fills the active level, and entering any
            // other one fills it then. Grabbing all five would be a QA-only behaviour.
        }
    }

    /** QA: delete downloaded lesson content so the download flow starts from zero again. */
    fun deleteDownloadedContent() {
        viewModelScope.launch { contentPackRepository.deleteAll() }
    }

    fun resetProgress(onDone: () -> Unit) {
        viewModelScope.launch {
            progressResetRepository.resetAllProgress()
            onDone()
        }
    }
}
