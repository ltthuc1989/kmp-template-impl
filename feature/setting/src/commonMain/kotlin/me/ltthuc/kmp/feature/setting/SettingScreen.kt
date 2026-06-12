package me.ltthuc.kmp.feature.setting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.model.MONETIZATION_ENABLED
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_cancel
import me.ltthuc.kmp.core.resource.common_confirm
import me.ltthuc.kmp.core.resource.setting_other_privacy_policy
import me.ltthuc.kmp.core.resource.setting_other_team_of_service
import me.ltthuc.kmp.core.resource.setting_reset_confirm_message
import me.ltthuc.kmp.core.resource.setting_reset_confirm_title
import me.ltthuc.kmp.core.resource.setting_reset_progress
import me.ltthuc.kmp.core.resource.setting_share_message
import me.ltthuc.kmp.core.ui.dialog.ParentalGateScreen
import me.ltthuc.kmp.core.ui.screen.Destination
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.setting.components.SettingActionButton
import me.ltthuc.kmp.feature.setting.components.SettingTopAppBar
import me.ltthuc.kmp.feature.setting.components.section.SettingGeneralSection
import me.ltthuc.kmp.feature.setting.components.section.SettingOthersSection
import me.ltthuc.kmp.feature.setting.components.section.SettingPaywallSection
import me.ltthuc.kmp.feature.setting.components.section.SettingSupportSection
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val SUPPORT_URL = "https://ltthuc1989.github.io/phonics-kids/"

@Composable
internal fun SettingScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingViewModel = koinViewModel(),
) {
    val navBackStack = LocalNavBackStack.current
    val uriHandler = LocalUriHandler.current
    val setting by viewModel.setting.collectAsStateWithLifecycle()
    val shareMessage = stringResource(Res.string.setting_share_message)

    var showParentalGate by remember { mutableStateOf(false) }
    var showResetGate by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val isRootTab = navBackStack.size <= 1

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                item {
                    SettingGeneralSection(
                        modifier = Modifier.fillMaxWidth(),
                        sfxEnabled = setting.sfxEnabled,
                        onSfxEnabledChanged = viewModel::setSfxEnabled,
                    )
                }

                item {
                    SettingSupportSection(
                        modifier = Modifier.fillMaxWidth(),
                        onFeedback = { uriHandler.openUri(SUPPORT_URL) },
                        onShare = { viewModel.shareApp(shareMessage) },
                        onRate = viewModel::rateApp,
                    )
                }

                if (MONETIZATION_ENABLED && (!setting.plusMode || setting.developerMode)) {
                    item {
                        SettingPaywallSection(
                            modifier = Modifier.fillMaxWidth(),
                            onUpgradeClicked = { showParentalGate = true },
                        )
                    }
                }

                item {
                    SettingOthersSection(
                        modifier = Modifier.fillMaxWidth(),
                        setting = setting,
                        onOpenSourceLicenseClicked = {
                            navBackStack.add(Destination.Setting.License)
                        },
                        onDeveloperModeChanged = viewModel::setDeveloperMode,
                        onShowSpeakButtonChanged = viewModel::setShowSpeakButton,
                        onThemeChanged = viewModel::setTheme,
                        onPaletteChanged = viewModel::setAppThemePalette,
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

                item {
                    Spacer(Modifier.height(24.dp))
                    SettingActionButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(Res.string.setting_reset_progress),
                        color = MaterialTheme.colorScheme.error,
                        onClick = { showResetGate = true },
                    )
                }

                item {
                    SettingsFooter(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        onTerms = { uriHandler.openUri(SUPPORT_URL) },
                        onPrivacy = { uriHandler.openUri(SUPPORT_URL) },
                    )
                }
            }
        }

        if (showParentalGate) {
            ParentalGateScreen(
                modifier = Modifier.fillMaxSize(),
                onPass = {
                    showParentalGate = false
                    navBackStack.add(Destination.Paywall(Destination.Paywall.Source.SETTINGS))
                },
                onDismiss = { showParentalGate = false },
            )
        }

        if (showResetGate) {
            ParentalGateScreen(
                modifier = Modifier.fillMaxSize(),
                onPass = {
                    showResetGate = false
                    showResetConfirm = true
                },
                onDismiss = { showResetGate = false },
            )
        }

        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text(stringResource(Res.string.setting_reset_confirm_title)) },
                text = { Text(stringResource(Res.string.setting_reset_confirm_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            viewModel.resetProgress {
                                navBackStack.clear()
                                navBackStack.add(Destination.Home)
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(Res.string.common_confirm),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) {
                        Text(stringResource(Res.string.common_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsFooter(
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        Text(
            modifier = Modifier.clickable(onClick = onTerms),
            text = stringResource(Res.string.setting_other_team_of_service),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.Underline,
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            modifier = Modifier.clickable(onClick = onPrivacy),
            text = stringResource(Res.string.setting_other_privacy_policy),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.Underline,
        )
    }
}
