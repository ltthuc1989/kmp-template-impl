package me.ltthuc.kmp.feature.onboarding.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_skip
import me.ltthuc.kmp.core.resource.onboarding_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OnboardingTopBar(
    showBack: Boolean,
    showTitle: Boolean,
    onBackClicked: () -> Unit,
    onSkipClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        if (showBack) {
            IconButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (showTitle) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(Res.string.onboarding_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        TextButton(
            onClick = onSkipClicked,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .heightIn(min = 64.dp),
        ) {
            Text(
                text = stringResource(Res.string.common_skip),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
