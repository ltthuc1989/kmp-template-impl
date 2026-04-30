package me.ltthuc.kmp.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.repository.AppSettingRepository

class OnboardingViewModel(
    private val repository: AppSettingRepository,
) : ViewModel() {
    val setting = repository.setting

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setHasSeenOnboarding(true)
        }
    }
}
