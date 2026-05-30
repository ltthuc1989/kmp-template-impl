package me.ltthuc.kmp.feature.learningpath.game.memorymatch.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.ltthuc.kmp.core.ui.screen.AsyncLoadContents
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.OceanDecorations
import me.ltthuc.kmp.feature.learningpath.game.common.OceanBackground
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.MemoryMatchViewModel
import me.ltthuc.kmp.feature.learningpath.game.memorymatch.view.MemoryGrid
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Dev preview for MemoryMatch — loads real lessons via the VM with a hard-coded L1U1 unit
 * so the developer can tap cards + see flip / match animations without going through the
 * full lesson flow. Reachable from Setting → Developer mode → "Memory Match Preview".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemoryMatchPreviewScreen(
    modifier: Modifier = Modifier,
    viewModel: MemoryMatchViewModel = koinViewModel(key = PREVIEW_UNIT_ID) {
        parametersOf(PREVIEW_UNIT_ID)
    },
) {
    val navBackStack = LocalNavBackStack.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Memory Match Preview") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.lastIndex)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        AsyncLoadContents(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            screenState = screenState,
            containerColor = Color.Transparent,
        ) { ui ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brush = OceanBackground),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        OceanDecorations()
                        MemoryGrid(
                            cards = ui.cards,
                            matchedIds = ui.matchedIds,
                            selectedIds = ui.selectedIds,
                            enabled = !ui.isResolving && !ui.isComplete,
                            onCardTap = viewModel::onCardTapped,
                        )
                    }
                }
            }
        }
    }
}

private const val PREVIEW_UNIT_ID = "L1U1"
