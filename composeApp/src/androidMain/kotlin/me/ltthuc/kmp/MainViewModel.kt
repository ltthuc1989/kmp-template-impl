package me.ltthuc.kmp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.repository.AppSettingRepository
import me.ltthuc.kmp.core.repository.BillingRepository
import me.ltthuc.kmp.core.repository.ContentPackRepository

class MainViewModel(
    private val settingRepository: AppSettingRepository,
    private val billingRepository: BillingRepository,
    private val contentPackRepository: ContentPackRepository,
) : ViewModel() {

    val setting = settingRepository.setting

    private val _isAdsSdkInitialized = MutableStateFlow(false)
    val isAdsSdkInitialized = _isAdsSdkInitialized.asStateFlow()

    init {
        billingRepository.configure()

        viewModelScope.launch {
            settingRepository.initializeIdIfNeeded()
            // Clears what an app update leaves behind: the pre-pack audio cache, and pack files
            // superseded by new content. Nothing else would ever remove either.
            contentPackRepository.cleanUpAfterUpdate()
        }
    }

    fun setAdsSdkInitialized() {
        _isAdsSdkInitialized.value = true
    }
}
