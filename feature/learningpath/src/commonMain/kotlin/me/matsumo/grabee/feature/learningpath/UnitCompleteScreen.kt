package me.matsumo.grabee.feature.learningpath

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.matsumo.grabee.core.ui.theme.LocalNavBackStack

@Composable
internal fun UnitCompleteScreen(
    levelId: String,
    unitId: String,
    starsEarned: Int,
    modifier: Modifier = Modifier,
) {
    val navBackStack = LocalNavBackStack.current

    Scaffold(modifier = modifier) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Design pending — UnitComplete")
            Spacer(Modifier.height(8.dp))
            Text("Unit: $unitId")
            Text("Stars earned: $starsEarned / 24")
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = {
                    while (navBackStack.size > 0 &&
                        navBackStack.last() !is me.matsumo.grabee.core.ui.screen.Destination.Learning.UnitSelection
                    ) {
                        navBackStack.removeAt(navBackStack.size - 1)
                    }
                }) {
                    Text("Back to Units")
                }
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    while (navBackStack.size > 0 &&
                        navBackStack.last() !is me.matsumo.grabee.core.ui.screen.Destination.Learning.UnitSelection
                    ) {
                        navBackStack.removeAt(navBackStack.size - 1)
                    }
                    Unit // placeholder for next-unit push — real logic khi có design
                }) {
                    Text("Next Unit (temp)")
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("levelId: $levelId (for future next-unit logic)")
        }
    }
}
