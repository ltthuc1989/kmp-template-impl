package me.ltthuc.kmp.update

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.update_ready_action
import me.ltthuc.kmp.core.resource.update_ready_message
import me.ltthuc.kmp.core.resource.update_ready_title
import me.ltthuc.kmp.core.ui.theme.LocalAppLanguage
import me.ltthuc.kmp.core.ui.theme.LocalAppLocale
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom banner offering the restart that finishes a downloaded flexible update.
 *
 * Deliberately not a dialog: the update is already on the device and nothing breaks by
 * ignoring it, so it must not interrupt a child mid-lesson. It floats above the nav host
 * without consuming touches anywhere else on the screen.
 *
 * The text is aimed at whoever decides to restart the app, which is a parent — so it opts
 * out of the app-wide English lock the way the other parent surfaces do (Settings, Paywall,
 * Onboarding) and follows the chosen app language.
 */
@Composable
internal fun UpdateReadyBanner(
    visible: Boolean,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            CompositionLocalProvider(LocalAppLocale provides LocalAppLanguage.current) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.update_ready_title),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(Res.string.update_ready_message),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Text(
                            text = stringResource(Res.string.update_ready_action),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.inversePrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable(onClick = onRestart)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
