package me.ltthuc.kmp.feature.setting.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.setting_other_developer_mode_dialog_description
import me.ltthuc.kmp.core.resource.setting_other_developer_mode_dialog_title
import me.ltthuc.kmp.core.ui.theme.LocalAppConfig
import org.jetbrains.compose.resources.stringResource

/**
 * PIN prompt shared by the debug-only Developer Mode switch and the hidden reviewer unlock in
 * the Settings footer. It only reports that the PIN matched; what that unlocks is the caller's
 * decision.
 */
@Composable
internal fun SettingDeveloperModeDialog(
    onPinAccepted: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.setting_other_developer_mode_dialog_title),
    description: String = stringResource(Res.string.setting_other_developer_mode_dialog_description),
) {
    val appConfig = LocalAppConfig.current

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(pin) {
        error = false
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = title)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = description)

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = pin,
                    onValueChange = { pin = it },
                    label = {
                        Text("PIN")
                    },
                    supportingText = {
                        if (error) {
                            Text("Invalid PIN")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin == appConfig.developerPin) {
                        onPinAccepted.invoke()
                    } else {
                        error = true
                    }
                },
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
