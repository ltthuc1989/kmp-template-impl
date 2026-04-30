package me.ltthuc.kmp.feature.onboarding

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

@Immutable
data class OnboardingPageData(
    val title: StringResource,
    val subtitle: StringResource,
    val illustration: ImageVector,
    val badge: ImageVector,
)
