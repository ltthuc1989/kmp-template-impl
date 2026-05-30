package me.ltthuc.kmp.feature.learningpath.game.bubblepop.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.toImmutableList
import me.ltthuc.kmp.core.ui.theme.LocalNavBackStack
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.spawnBubblesForRound
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.BubbleCanvas
import me.ltthuc.kmp.feature.learningpath.game.bubblepop.view.OceanDecorations

/**
 * Standalone dev preview of the bubble game UI. Lets the developer:
 * - Tap bubbles and watch the pop/shake animations without going through the full unit flow.
 * - Toggle slow-motion to inspect physics.
 * - Change the target letter via the chip row.
 * - Restart (respawn fresh bubbles) at any time.
 *
 * Reachable from Settings → enter Developer PIN → "Bubble Pop Preview".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BubblePopPreviewScreen(
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    var targetLetter by remember { mutableStateOf(DEFAULT_TARGET) }
    var seed by remember { mutableIntStateOf(0) }
    var slowMo by remember { mutableStateOf(false) }
    var tapStats by remember { mutableStateOf(TapStats(0, 0)) }

    val bubbles by remember(targetLetter, seed) {
        derivedStateOf {
            // Use unit letters + a few extras so distractors visually mirror the screenshot (B/E/A).
            spawnBubblesForRound(
                targetLetter = targetLetter,
                unitLetters = DEFAULT_UNIT_LETTERS,
            ).toImmutableList()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Bubble Pop Preview") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (navBackStack.size > 1) navBackStack.removeAt(navBackStack.lastIndex)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        seed += 1
                        tapStats = TapStats(0, 0)
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Restart")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(brush = oceanBackground()),
        ) {
            PreviewStatsBar(
                target = targetLetter,
                stats = tapStats,
                slowMo = slowMo,
                onSlowMoChanged = { slowMo = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                OceanDecorations()
                BubbleCanvas(
                    bubbles = bubbles,
                    onBubbleTapped = { _, isCorrect ->
                        tapStats = tapStats.copy(
                            correct = tapStats.correct + if (isCorrect) 1 else 0,
                            wrong = tapStats.wrong + if (!isCorrect) 1 else 0,
                        )
                    },
                    speedMultiplier = if (slowMo) 0.3f else 1.0f,
                )
            }

            TargetLetterChips(
                selected = targetLetter,
                onSelected = {
                    targetLetter = it
                    seed += 1
                    tapStats = TapStats(0, 0)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun PreviewStatsBar(
    target: String,
    stats: TapStats,
    slowMo: Boolean,
    onSlowMoChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = target,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Target", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Text(
                text = "✅ ${stats.correct}    ❌ ${stats.wrong}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Slow-mo", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Switch(checked = slowMo, onCheckedChange = onSlowMoChanged)
        }
    }
}

@Composable
private fun TargetLetterChips(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ALPHABET) { letter ->
            val isSelected = letter == selected
            Box(
                modifier = Modifier
                    .size(width = 44.dp, height = 44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White else Color.White.copy(alpha = 0.18f),
                    )
                    .clickable { onSelected(letter) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        }
    }
}

private fun oceanBackground(): Brush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0E7C8A),
        Color(0xFF1FA3B8),
        Color(0xFF38C7DC),
    ),
)

private data class TapStats(val correct: Int, val wrong: Int)

private const val DEFAULT_TARGET = "A"
private val DEFAULT_UNIT_LETTERS = listOf("A", "B", "C")
private val ALPHABET = ('A'..'Z').map { it.toString() }
