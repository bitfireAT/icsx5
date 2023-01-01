package at.bitfire.icsdroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.activity.MainActivity.Companion.Paths
import at.bitfire.icsdroid.ui.reusable.ColorPicker
import at.bitfire.icsdroid.ui.reusable.SwitchRow

@Composable
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
fun SubscriptionScreen(navHostController: NavHostController, subscription: Subscription) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var displayName by remember { mutableStateOf(subscription.displayName) }
    var color by remember { mutableStateOf(subscription.color) }
    var ignoreEmbeddedAlerts by remember { mutableStateOf(subscription.ignoreEmbeddedAlerts) }
    var defaultAlarmMinutes by remember { mutableStateOf(subscription.defaultAlarmMinutes) }

    var showDefaultAlarmPicker by remember { mutableStateOf(false) }
    var defaultAlarmMinutesDialog by remember { mutableStateOf("") }

    var dirty by remember { mutableStateOf(false) }

    fun checkDirty() {
        dirty = listOf(
            displayName to subscription.displayName,
            color to subscription.color,
            ignoreEmbeddedAlerts to subscription.ignoreEmbeddedAlerts,
            defaultAlarmMinutes to subscription.defaultAlarmMinutes,
        ).any { (a, b) -> a != b }
    }

    val dismissDialog = { showDefaultAlarmPicker = false }
    if (showDefaultAlarmPicker)
        AlertDialog(
            onDismissRequest = dismissDialog,
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
                        defaultAlarmMinutes = defaultAlarmMinutesDialog.toLong()
                        showDefaultAlarmPicker = false
                        checkDirty()
                    },
                ) {
                    Text(stringResource(R.string.default_alarm_dialog_set))
                }
            },
            dismissButton = {
                TextButton(onClick = dismissDialog) {
                    Text(stringResource(R.string.default_alarm_dialog_cancel))
                }
            },
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_edit_calendar)) },
                navigationIcon = {
                    IconButton(onClick = { Paths.Subscriptions.navigate(navHostController) }) {
                        Icon(
                            Icons.Rounded.KeyboardArrowLeft,
                            stringResource(R.string.edit_calendar_cancel),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            Icons.Rounded.Share,
                            stringResource(R.string.edit_calendar_send_url),
                        )
                    }
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            Icons.Rounded.Delete,
                            stringResource(R.string.edit_calendar_delete),
                        )
                    }
                    AnimatedVisibility(visible = dirty) {
                        IconButton(onClick = { TODO() }) {
                            Icon(
                                Icons.Rounded.Save,
                                stringResource(R.string.edit_calendar_save),
                            )
                        }
                    }
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = subscription.url,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = ContentAlpha.disabled),
            )
            Text(
                text = stringResource(R.string.add_calendar_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            TextField(
                value = displayName,
                onValueChange = { displayName = it; checkDirty() },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrect = true,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
                keyboardActions = KeyboardActions { keyboardController?.hide() },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    ColorPicker(color = color?.let { Color(it) }) { newColor ->
                        color = newColor.toArgb()
                        checkDirty()
                    }
                },
            )

            Text(
                text = stringResource(R.string.add_calendar_alarms_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            SwitchRow(
                title = stringResource(R.string.add_calendar_alarms_ignore_title),
                subtitle = stringResource(R.string.add_calendar_alarms_ignore_description),
                checked = ignoreEmbeddedAlerts,
                onCheckedChanged = { ignoreEmbeddedAlerts = it; checkDirty() },
            )
            SwitchRow(
                title = stringResource(R.string.add_calendar_alarms_default_title),
                checked = defaultAlarmMinutes != null,
                onCheckedChanged = { checked ->
                    if (!checked)
                        defaultAlarmMinutes = null
                    else {
                        defaultAlarmMinutesDialog = ""
                        showDefaultAlarmPicker = true
                    }
                },
            )
        }
    }
}
