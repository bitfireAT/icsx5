package at.bitfire.icsdroid.ui.pages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.model.SubscriptionDetailsModel
import at.bitfire.icsdroid.ui.reusable.ColorPicker
import at.bitfire.icsdroid.ui.reusable.RequiresAuthCard
import at.bitfire.icsdroid.ui.reusable.SwitchRow

@Composable
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
fun SubscriptionDetailsPage(model: SubscriptionDetailsModel) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var displayName by model.displayName
    var color by model.color
    var ignoreEmbeddedAlerts by model.ignoreEmbeddedAlerts
    var defaultAlarm by model.defaultAlarm

    val fieldsEnabled by model.fieldsEnabled

    val url by model.uri

    var defaultAlarmTemp by remember { mutableStateOf("") }
    var showDefaultAlarmDialog by remember { mutableStateOf(false) }

    if (showDefaultAlarmDialog) {
        val defaultAlarmValid = defaultAlarmTemp.toLongOrNull()?.let { it > 0 } ?: false
        AlertDialog(
            onDismissRequest = { showDefaultAlarmDialog = false },
            title = { Text(stringResource(R.string.default_alarm_dialog_title)) },
            text = {
                TextField(
                    value = defaultAlarmTemp,
                    onValueChange = { defaultAlarmTemp = it },
                    label = { Text(stringResource(R.string.default_alarm_dialog_message)) },
                    isError = !defaultAlarmValid,
                    supportingText = {
                        Text(
                            stringResource(
                                if (defaultAlarmValid)
                                    R.string.default_alarm_dialog_hint
                                else
                                    R.string.default_alarm_dialog_error,
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions { keyboardController?.hide() },
                    singleLine = true,
                    maxLines = 1,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = defaultAlarmValid,
                    onClick = {
                        defaultAlarm = defaultAlarmTemp.toLong()
                        showDefaultAlarmDialog = false
                    },
                ) { Text(stringResource(R.string.default_alarm_dialog_set)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDefaultAlarmDialog = false },
                ) { Text(stringResource(R.string.default_alarm_dialog_cancel)) }
            },
        )
    }

    Text(
        text = url?.toString() ?: "",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(ContentAlpha.disabled),
    )

    OutlinedTextField(
        value = displayName,
        onValueChange = { displayName = it },
        modifier = Modifier
            .fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            autoCorrect = true,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions { },
        singleLine = true,
        maxLines = 1,
        enabled = fieldsEnabled,
        label = { Text(stringResource(R.string.add_calendar_title_hint)) },
        leadingIcon = {
            ColorPicker(
                color = color,
                modifier = Modifier
                    .padding(8.dp),
                enabled = fieldsEnabled,
            ) { color = it }
        },
    )

    SwitchRow(
        title = stringResource(R.string.add_calendar_alarms_ignore_title),
        subtitle = stringResource(R.string.add_calendar_alarms_ignore_description),
        checked = ignoreEmbeddedAlerts,
        onCheckedChanged = { ignoreEmbeddedAlerts = it },
        enabled = fieldsEnabled,
    )
    SwitchRow(
        title = stringResource(R.string.add_calendar_alarms_default_title),
        subtitle = defaultAlarm?.let {
            stringResource(
                R.string.add_calendar_alarms_default_description,
                it
            )
        },
        checked = defaultAlarm != null,
        onCheckedChanged = { addingAlarm ->
            if (addingAlarm) {
                defaultAlarmTemp = ""
                showDefaultAlarmDialog = true
            } else defaultAlarm = null
        },
        enabled = fieldsEnabled,
    )

    RequiresAuthCard(
        requiresAuthState = model.requiresAuth,
        usernameState = model.username,
        passwordState = model.password,
        enabled = fieldsEnabled,
    )
}
