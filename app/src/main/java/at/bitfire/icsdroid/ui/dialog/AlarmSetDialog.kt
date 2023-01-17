package at.bitfire.icsdroid.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import at.bitfire.icsdroid.R

@ExperimentalMaterial3Api
@Composable
@ExperimentalComposeUiApi
fun AlarmSetDialog(onDismissRequest: () -> Unit, onTimePicked: (newTime: Long) -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var defaultAlarmMinutesDialog by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.default_alarm_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.default_alarm_dialog_message),
                    Modifier.fillMaxWidth(),
                )
                TextField(
                    value = defaultAlarmMinutesDialog,
                    onValueChange = { t ->
                        t.toLongOrNull()
                            ?.takeIf { it > 0 }
                            ?.let { defaultAlarmMinutesDialog = it.toString() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.default_alarm_dialog_hint)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions {
                        keyboardController?.hide()
                    },
                    singleLine = true,
                    maxLines = 1,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = defaultAlarmMinutesDialog.isNotBlank() && defaultAlarmMinutesDialog.toLongOrNull() != null,
                onClick = {
                    onTimePicked(defaultAlarmMinutesDialog.toLong())
                },
            ) {
                Text(stringResource(R.string.default_alarm_dialog_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.default_alarm_dialog_cancel))
            }
        },
    )
}
