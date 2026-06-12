package me.ltthuc.kmp.core.ui.dialog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_back
import me.ltthuc.kmp.core.resource.parental_gate_backspace
import me.ltthuc.kmp.core.resource.parental_gate_subtitle
import me.ltthuc.kmp.core.resource.parental_gate_title
import me.ltthuc.kmp.core.ui.components.PuffySurface
import me.ltthuc.kmp.core.ui.theme.bold
import me.ltthuc.kmp.core.ui.theme.extraBold
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.random.Random

private const val GATE_DIGIT_COUNT = 4

/**
 * Full-screen Duolingo-style parental gate. Shows [GATE_DIGIT_COUNT] random digits spelled
 * out in words ("ZERO, FIVE, EIGHT, ONE"); the adult must tap them on the pad in that exact
 * order. A wrong tap clears progress and shakes. Completing the sequence calls [onPass].
 *
 * Designed to be unsolvable by young children (requires reading + sequencing words), satisfying
 * Apple Kids Category / Google Designed-for-Families parental-gate requirements. Themed with the
 * app's brand colors via [MaterialTheme.colorScheme].
 */
@Composable
fun ParentalGateScreen(
    onPass: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Roll once; survive recomposition + rotation (the old math dialog re-rolled on recompose).
    val target = rememberSaveable(
        saver = listSaver(save = { it }, restore = { it }),
    ) { List(GATE_DIGIT_COUNT) { Random.nextInt(0, 10) } }

    val entered = remember { mutableStateListOf<Int>() }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var locked by remember { mutableStateOf(false) }

    val digitWords = (0..9).map { stringResource(digitWordRes(it)) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            // Top bar: back arrow + centered title.
            Box(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.common_back),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(Res.string.parental_gate_title),
                    style = MaterialTheme.typography.titleLarge.bold(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(Res.string.parental_gate_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = target.joinToString(", ") { digitWords[it].uppercase() },
                style = MaterialTheme.typography.headlineSmall.extraBold(),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(36.dp))

            // Dashes that fill in as the adult taps.
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset { IntOffset(shake.value.roundToInt(), 0) },
            ) {
                repeat(GATE_DIGIT_COUNT) { i ->
                    val filled = i < entered.size
                    Text(
                        text = if (filled) entered[i].toString() else "—",
                        style = MaterialTheme.typography.displaySmall.extraBold(),
                        color = if (filled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            NumberPad(
                onDigit = { d ->
                    if (locked) return@NumberPad
                    if (target[entered.size] == d) {
                        entered.add(d)
                        if (entered.size == GATE_DIGIT_COUNT) {
                            locked = true
                            onPass()
                        }
                    } else {
                        entered.clear()
                        scope.launch { shake.shakeAnimation() }
                    }
                },
                onBackspace = { if (entered.isNotEmpty()) entered.removeAt(entered.lastIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { d ->
                    PuffyKey(modifier = Modifier.weight(1f), onClick = { onDigit(d) }) {
                        KeyText(d.toString())
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.weight(1f))
            PuffyKey(modifier = Modifier.weight(1f), onClick = { onDigit(0) }) {
                KeyText("0")
            }
            PuffyKey(modifier = Modifier.weight(1f), onClick = onBackspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(Res.string.parental_gate_backspace),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun PuffyKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    PuffySurface(
        modifier = modifier
            .aspectRatio(KEY_ASPECT_RATIO)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 10.dp,
        shadowTint = MaterialTheme.colorScheme.primary,
        shadowAlpha = 0.45f,
        topHighlightHeight = 10.dp,
        topHighlightAlpha = 0.30f,
        bottomShadeHeight = 10.dp,
        bottomShadeAlpha = 0.28f,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun KeyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium.extraBold(),
        color = MaterialTheme.colorScheme.onPrimary,
    )
}

private suspend fun Animatable<Float, *>.shakeAnimation() {
    val k = 18f
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 350
            -k at 50
            k at 100
            -k at 150
            k at 200
            -k / 2 at 260
            0f at 350
        },
    )
}

private const val KEY_ASPECT_RATIO = 1.5f
