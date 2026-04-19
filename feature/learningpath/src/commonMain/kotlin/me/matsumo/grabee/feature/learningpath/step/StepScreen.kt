package me.matsumo.grabee.feature.learningpath.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.grabee.core.ui.screen.Destination
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StepScreen(
    levelId: String,
    unitId: String,
    stepIndex: Int,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current
    val stepName = stepName(stepIndex)
    val totalSteps = 8

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Step ${stepIndex + 1}/$totalSteps — $stepName") },
                navigationIcon = {
                    IconButton(onClick = { navBackStack.removeAt(navBackStack.size - 1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Design pending — $stepName")
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                val next = if (stepIndex < totalSteps - 1) {
                    Destination.Learning.Step(levelId, unitId, stepIndex + 1)
                } else {
                    Destination.Learning.UnitComplete(levelId, unitId, starsEarned = 24)
                }
                navBackStack.add(next)
            }) {
                Text("Continue")
            }
        }
    }
}

private fun stepName(stepIndex: Int): String = when (stepIndex) {
    0 -> "Sound Intro"
    1 -> "Chant"
    2 -> "Vocabulary"
    3 -> "Identify"
    4 -> "Blending"
    5 -> "Matching"
    6 -> "Tracing"
    7 -> "Story"
    else -> "Unknown Step $stepIndex"
}
