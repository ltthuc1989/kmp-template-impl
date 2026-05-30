package me.ltthuc.kmp.core.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Mốc 1: no-op stub. Original implementation loaded + showed AdMob rewarded ad;
 * archived in `marketing/mocs/moc1-ads-archive.md`.
 *
 * Calls [onUnavailable] immediately so callers fall back to non-reward path.
 */
@Composable
fun RewardedSkipLauncher(
    onRewardEarned: () -> Unit,
    onUnavailable: () -> Unit,
) {
    LaunchedEffect(Unit) { onUnavailable() }
}
