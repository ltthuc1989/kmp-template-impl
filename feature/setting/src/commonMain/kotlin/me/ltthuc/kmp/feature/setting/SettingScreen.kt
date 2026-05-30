package me.ltthuc.kmp.feature.setting

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED
import me.ltthuc.kmp.core.ui.dialog.ParentalGateDialog
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.setting.components.SettingSwitchItem
import me.ltthuc.kmp.feature.setting.components.SettingTopAppBar
import me.ltthuc.kmp.feature.setting.components.section.SettingInfoSection
import me.ltthuc.kmp.feature.setting.components.section.SettingOthersSection
import me.ltthuc.kmp.feature.setting.components.section.SettingPaywallSection
import me.ltthuc.kmp.feature.setting.components.section.SettingThemeSection
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current
    val uriHandler = LocalUriHandler.current
    val setting by viewModel.setting.collectAsStateWithLifecycle()
    var showParentalGate by remember { mutableStateOf(false) }

    val isRootTab = navBackStack.size <= 1

    if (showParentalGate) {
        ParentalGateDialog(
            onPass = {
                showParentalGate = false
                navBackStack.add(Destination.Paywall(Destination.Paywall.Source.SETTINGS))
            },
            onDismiss = { showParentalGate = false },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            SettingTopAppBar(
                onBackClicked = { navBackStack.removeAt(navBackStack.size - 1) },
                modifier = Modifier,
                showBackButton = !isRootTab,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it,
        ) {
            if (MONETIZATION_ENABLED && (!setting.plusMode || setting.developerMode)) {
                item {
                    SettingPaywallSection(
                        modifier = Modifier.fillMaxWidth(),
                        onUpgradeClicked = { showParentalGate = true },
                    )
                }
            }

            item {
                SettingThemeSection(
                    modifier = Modifier.fillMaxWidth(),
                    setting = setting,
                    onThemeChanged = viewModel::setTheme,
                    onPaletteChanged = viewModel::setAppThemePalette,
                )
            }

            item {
                SettingSwitchItem(
                    title = "Sound effects",
                    description = "Chime when you tap and on correct answers.",
                    value = setting.sfxEnabled,
                    onValueChanged = viewModel::setSfxEnabled,
                )
            }
            item {
                SettingSwitchItem(
                    title = "Voice praise",
                    description = "\"Great job!\", \"Try again!\". Does not affect letter sounds.",
                    value = setting.voiceEnabled,
                    onValueChanged = viewModel::setVoiceEnabled,
                )
            }
            item {
                SettingSwitchItem(
                    title = "Background music",
                    description = "Soft music loop on menu screens.",
                    value = setting.musicEnabled,
                    onValueChanged = viewModel::setMusicEnabled,
                )
            }

            item {
                SettingInfoSection(
                    modifier = Modifier.fillMaxWidth(),
                    setting = setting,
                )
            }

            item {
                SettingOthersSection(
                    modifier = Modifier.fillMaxWidth(),
                    setting = setting,
                    onTeamsOfServiceClicked = {
                        uriHandler.openUri("https://ltthuc1989.github.io/phonics-kids/")
                    },
                    onPrivacyPolicyClicked = {
                        uriHandler.openUri("https://ltthuc1989.github.io/phonics-kids/")
                    },
                    onOpenSourceLicenseClicked = {
                        navBackStack.add(Destination.Setting.License)
                    },
                    onDeveloperModeChanged = viewModel::setDeveloperMode,
                    onShowSpeakButtonChanged = viewModel::setShowSpeakButton,
                    onLetterGuideDebugClicked = {
                        navBackStack.add(Destination.Learning.TracingGuideDebug)
                    },
                    onBubblePopPreviewClicked = {
                        navBackStack.add(Destination.Learning.BubblePopPreview)
                    },
                    onMemoryMatchPreviewClicked = {
                        navBackStack.add(Destination.Learning.MemoryMatchPreview)
                    },
                )
            }
        }
    }
}
