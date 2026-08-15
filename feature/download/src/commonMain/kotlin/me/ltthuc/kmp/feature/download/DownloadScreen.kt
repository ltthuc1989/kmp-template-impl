package me.ltthuc.kmp.feature.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.content_download_action
import me.ltthuc.kmp.core.resource.content_download_done
import me.ltthuc.kmp.core.resource.content_download_failed_message
import me.ltthuc.kmp.core.resource.content_download_failed_title
import me.ltthuc.kmp.core.resource.content_download_later
import me.ltthuc.kmp.core.resource.content_download_message
import me.ltthuc.kmp.core.resource.content_download_progress
import me.ltthuc.kmp.core.resource.content_download_ready_message
import me.ltthuc.kmp.core.resource.content_download_ready_title
import me.ltthuc.kmp.core.resource.content_download_retry
import me.ltthuc.kmp.core.resource.content_download_title
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Asks a parent to fetch a level's lesson content, then shows how it is going.
 *
 * The size is always on the button rather than buried in a settings toggle: at ~9MB a level
 * a "Wi-Fi only" switch costs more than it saves, and a parent deciding on mobile data needs
 * the number in front of them, not a policy.
 */
@Composable
internal fun DownloadScreen(
    levelId: String,
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = koinViewModel(key = levelId) { parametersOf(levelId) },
) {
    val navBackStack = LocalNavBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val close: () -> Unit = { if (navBackStack.isNotEmpty()) navBackStack.removeAt(navBackStack.lastIndex) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (val state = uiState) {
            DownloadUiState.Loading -> Unit

            is DownloadUiState.Pending -> Prompt(
                bytes = state.bytes,
                onDownload = viewModel::start,
                onLater = close,
            )

            is DownloadUiState.Running -> Running(state)

            DownloadUiState.Ready -> Finished(onDone = close)

            is DownloadUiState.Failed -> Failure(
                bytes = state.bytes,
                onRetry = viewModel::start,
                onLater = close,
            )
        }
    }
}

@Composable
private fun Prompt(bytes: Long, onDownload: () -> Unit, onLater: () -> Unit) {
    Icon(
        imageVector = Icons.Rounded.CloudDownload,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(72.dp),
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(Res.string.content_download_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.content_download_message, formatBytes(bytes)),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.content_download_action, formatBytes(bytes)))
    }
    TextButton(onClick = onLater) {
        Text(stringResource(Res.string.content_download_later))
    }
}

@Composable
private fun Running(state: DownloadUiState.Running) {
    val fraction by animateFloatAsState(targetValue = state.fraction, label = "downloadProgress")

    Text(
        text = stringResource(Res.string.content_download_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
    LinearProgressIndicator(
        progress = { fraction },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(Res.string.content_download_progress, state.filesDone, state.filesTotal),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Finished(onDone: () -> Unit) {
    Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(72.dp),
    )
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(Res.string.content_download_ready_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.content_download_ready_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.content_download_done))
    }
}

@Composable
private fun Failure(bytes: Long, onRetry: () -> Unit, onLater: () -> Unit) {
    Text(
        text = stringResource(Res.string.content_download_failed_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(Res.string.content_download_failed_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.content_download_retry, formatBytes(bytes)))
    }
    TextButton(onClick = onLater) {
        Text(stringResource(Res.string.content_download_later))
    }
}

private const val BYTES_PER_MB = 1024f * 1024f

/** "8.9 MB" — one decimal is enough for a number meant to inform a spending decision. */
internal fun formatBytes(bytes: Long): String {
    val mb = bytes / BYTES_PER_MB
    val rounded = (mb * 10).toInt() / 10f
    return "$rounded MB"
}
