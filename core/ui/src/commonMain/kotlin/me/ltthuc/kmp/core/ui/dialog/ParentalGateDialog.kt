package me.ltthuc.kmp.core.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.common_cancel
import me.ltthuc.kmp.core.resource.common_next
import me.ltthuc.kmp.core.resource.parental_gate_error
import me.ltthuc.kmp.core.resource.parental_gate_hint
import me.ltthuc.kmp.core.resource.parental_gate_question
import me.ltthuc.kmp.core.resource.parental_gate_title
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

/**
 * Math problem dialog ("What is 5 + 7?") that blocks kids from accidentally triggering
 * purchases or external links. Required for Apple Kids Category and Google Designed for
 * Families. Operands re-roll on each composition.
 */
@Composable
fun ParentalGateDialog(
    onPass: () -> Unit,
    onDismiss: () -> Unit,
) {
    val a = remember { Random.nextInt(2, 9) }
    val b = remember { Random.nextInt(2, 9) }
    val correct = a + b
    var input by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val isCorrect = input.toIntOrNull() == correct
    val showError = attempted && input.isNotEmpty() && !isCorrect

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.parental_gate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.parental_gate_question, a, b))
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        input = raw.filter(Char::isDigit).take(3)
                        attempted = true
                    },
                    label = { Text(stringResource(Res.string.parental_gate_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = showError,
                    singleLine = true,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (showError) {
                    Text(text = stringResource(Res.string.parental_gate_error))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isCorrect,
                onClick = onPass,
            ) { Text(stringResource(Res.string.common_next)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.common_cancel)) }
        },
    )
}
