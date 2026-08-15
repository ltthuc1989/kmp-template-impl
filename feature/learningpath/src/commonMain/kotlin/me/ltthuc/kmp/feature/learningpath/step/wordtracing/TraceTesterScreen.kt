package me.ltthuc.kmp.feature.learningpath.step.wordtracing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dev-only interactive trace tester. Pick any lowercase letter (a–z) and trace it with the exact
 * word-trace engine ([LetterTraceCanvas]) so stroke order/direction/shape can be validated against
 * the Duolingo reference while authoring [DuolingoGlyphs] (the Level-2 glyph set, kept separate
 * from Level 1's LetterPaths). Access via Settings → Developer mode → "Trace tester".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceTesterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var retraceCount by remember { mutableStateOf(0) }
    var completed by remember { mutableStateOf(false) }

    val char = 'a' + selectedIndex
    val guide = remember(char) { DuolingoGlyphs.get(char) }

    fun retrace() {
        retraceCount += 1
        completed = false
    }

    fun selectIndex(index: Int) {
        selectedIndex = ((index % 26) + 26) % 26
        completed = false
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trace tester") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            // Prev / next / retrace controls (lowercase a–z only — this is the Level-2 Duolingo set).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Pill(label = "‹ Prev", selected = false) { selectIndex(selectedIndex - 1) }
                Pill(label = "Next ›", selected = false) { selectIndex(selectedIndex + 1) }
                Spacer(Modifier.weight(1f))
                Pill(label = "↺ Retrace", selected = false) { retrace() }
            }

            Spacer(Modifier.height(8.dp))

            // A–Z quick picker.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items((0..25).toList()) { index ->
                    Pill(label = ('a' + index).toString(), selected = index == selectedIndex) {
                        selectIndex(index)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                LetterTraceCanvas(
                    guide = guide,
                    resetKey = "$char:$retraceCount",
                    onLetterComplete = { completed = true },
                    modifier = Modifier.fillMaxSize(),
                )
                if (completed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("✓", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Debug info: stroke count + raw svgPaths (compare with Duolingo reference).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = "'$char' — ${guide.strokes.size} stroke(s)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                guide.strokes.forEachIndexed { i, stroke ->
                    Text(
                        text = "${i + 1}. ${stroke.svgPath}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}
