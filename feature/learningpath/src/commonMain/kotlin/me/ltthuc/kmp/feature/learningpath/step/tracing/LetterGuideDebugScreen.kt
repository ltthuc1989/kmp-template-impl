package me.ltthuc.kmp.feature.learningpath.step.tracing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Dev-only visual QA screen that renders all 52 Zaner-Bloser letter guides in a 4-column grid.
 * Access via Settings → Developer mode → "Letter guide QA" (see SettingOthersSection).
 *
 * Tap any tile to toggle its animation progress between 0 (dashed preview) and 1 (fully filled),
 * so you can verify the stroke-fill animation matches the intended shape.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LetterGuideDebugScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val letters = ('A'..'Z').toList()

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Letter guide QA (52 guides)") },
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
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader("Uppercase (A–Z)") }
            items(letters.chunked(4)) { row ->
                TileRow(row, uppercase = true)
            }
            item { Spacer(Modifier.height(16.dp)) }
            item { SectionHeader("Lowercase (a–z)") }
            items(letters.chunked(4)) { row ->
                TileRow(row, uppercase = false)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp),
    )
}

@Composable
@Suppress("UnstableCollections")
private fun TileRow(chars: List<Char>, uppercase: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chars.forEach { char ->
            Box(modifier = Modifier.weight(1f)) {
                GuideTile(guide = LetterPaths.get(char, uppercase))
            }
        }
        repeat(4 - chars.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun GuideTile(guide: LetterGuide) {
    val playAnim = remember(guide.char, guide.uppercase) { Animatable(0f) }
    val progress = playAnim.value
    val primary = MaterialTheme.colorScheme.primary
    val halo = MaterialTheme.colorScheme.primaryContainer
    val ghostColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = GHOST_TILE_ALPHA)
    var tileSize by remember { mutableStateOf(Size.Zero) }
    val scope = rememberCoroutineScope()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                tileSize = size
                drawLetterGuide(
                    guide = guide,
                    canvasSize = size,
                    animationProgress = progress,
                    primaryColor = primary,
                    haloColor = halo,
                    strokeWidthPx = 10f,
                    dashStrokeWidthPx = 1.5f,
                    showArrows = true,
                )
            }
            if (tileSize != Size.Zero) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    StrokeNumberBadges(
                        guide = guide,
                        canvasSizePx = tileSize,
                        badgeDiameter = 10,
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd,
            ) {
                ProgressToggleButton(progress = progress) {
                    scope.launch {
                        if (progress >= 1f) {
                            playAnim.snapTo(0f)
                        } else {
                            playAnim.snapTo(0f)
                            playAnim.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = guide.strokes.size * MS_PER_STROKE,
                                    easing = LinearEasing,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // Ghost-letter QA preview — same render path as the practice canvas in TracingScreen,
        // so any cap/alpha/overlap regression in drawGhostLetter shows up here for all 52 letters.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                val ghostStrokeWidthPx = minOf(size.width, size.height) * GHOST_TILE_STROKE_FRACTION
                drawGhostLetter(
                    guide = guide,
                    canvasSize = size,
                    color = ghostColor,
                    strokeWidthPx = ghostStrokeWidthPx,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${guide.char} (${guide.strokes.size})",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val GHOST_TILE_ALPHA = 0.45f
private const val GHOST_TILE_STROKE_FRACTION = 0.14f

private const val MS_PER_STROKE = 1200

@Composable
private fun ProgressToggleButton(progress: Float, onClick: () -> Unit) {
    val label = if (progress >= 1f) "✓" else "▶"
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(20.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
