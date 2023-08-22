package at.bitfire.icsdroid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.compose.viewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.ui.reusable.ColorCircle
import at.bitfire.icsdroid.ui.reusable.SwitchRow
import at.bitfire.icsdroid.ui.subscription.SubscriptionSettingsModel
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.color.ColorPalette
import com.vanpra.composematerialdialogs.color.colorChooser
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import org.joda.time.Minutes
import org.joda.time.format.PeriodFormat

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun SubscriptionSettings_TitleAndColor(
    subscriptionSettingsModel: SubscriptionSettingsModel = viewModel()
) {
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    Text(
        text = stringResource(R.string.add_calendar_title),
        style = MaterialTheme.typography.h5
    )

    val url by subscriptionSettingsModel.url.observeAsState("")
    val title by subscriptionSettingsModel.title.observeAsState("")
    val color by subscriptionSettingsModel.color.map { Color(it) }.observeAsState(Color.Black)

    Card(elevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                SelectionContainer {
                    Text(
                        text = url,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.caption
                    )
                }
                TextField(
                    value = title,
                    onValueChange = { subscriptionSettingsModel.title.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.add_calendar_title_hint)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        autoCorrect = true,
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions { softwareKeyboard?.hide() },
                    singleLine = true,
                    maxLines = 1
                )
            }

            val dialogState = rememberMaterialDialogState()
            MaterialDialog(
                dialogState,
                buttons = {
                    positiveButton(res = R.string.action_close) { dialogState.hide() }
                }
            ) {
                val subColorPalette = remember { ColorPalette.PrimarySub.toMutableList() }
                val colorPalette = remember {
                    ColorPalette.Primary.toMutableList().apply {
                        if (!contains(color)) {
                            add(color)
                            subColorPalette.add(listOf(color))
                        }
                    }
                }

                colorChooser(
                    colors = colorPalette,
                    subColors = subColorPalette,
                    waitForPositiveButton = false,
                    // Select by default the color selected. If the color is not in the palette,
                    // it will be available as the last one, as added in colorPalette
                    initialSelection = colorPalette.indexOf(color)
                        .takeIf { it >= 0 }
                        ?: (colorPalette.size - 1)
                ) {
                    subscriptionSettingsModel.color.value = it.toArgb()
                }
            }

            ColorCircle(
                color = color,
                size = 40.dp,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable { dialogState.show() }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubscriptionSettings_AlertDialog(liveData: MutableLiveData<Long>, onDismissRequest: () -> Unit) {
    val softwareKeyboard = LocalSoftwareKeyboardController.current

    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    liveData.value = value.toLongOrNull()
                    onDismissRequest()
                },
                enabled = value.toLongOrNull() != null
            ) {
                Text(stringResource(R.string.default_alarm_dialog_set))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
        title = { Text(stringResource(R.string.default_alarm_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.default_alarm_dialog_message))

                // FIXME - Add info message when error (R.string.default_alarm_dialog_error)
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.default_alarm_dialog_hint)) },
                    singleLine = true,
                    maxLines = 1,
                    isError = value.toLongOrNull() == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions { softwareKeyboard?.hide() }
                )
            }
        }
    )
}

@Composable
fun SubscriptionSettings_Alarms(
    subscriptionSettingsModel: SubscriptionSettingsModel = viewModel()
) {
    /**
     * Must be set to a LiveData from [subscriptionSettingsModel]. Dialog will show, and the value
     * introduced by the user will be passed to this LiveData. When dialog is dismissed this is
     * updated to null.
     */
    var settingAlarm by remember { mutableStateOf<MutableLiveData<Long>?>(null) }
    settingAlarm?.let { liveData ->
        SubscriptionSettings_AlertDialog(liveData) { settingAlarm = null }
    }

    fun onCheckedChange(checked: Boolean, minutesLiveData: MutableLiveData<Long>) {
        if (!checked) {
            minutesLiveData.value = null
        } else {
            settingAlarm = minutesLiveData
        }
    }

    Text(
        text = stringResource(R.string.add_calendar_alarms_title),
        style = MaterialTheme.typography.h5,
        modifier = Modifier.padding(top = 16.dp)
    )

    val ignoreAlerts by subscriptionSettingsModel.ignoreAlerts.observeAsState()
    val defaultAlarm by subscriptionSettingsModel.defaultAlarmMinutes.observeAsState()
    val defaultAllDayAlarm by subscriptionSettingsModel.defaultAllDayAlarmMinutes.observeAsState()

    SwitchRow(
        checked = ignoreAlerts ?: false,
        enabled = ignoreAlerts != null,
        onCheckedChange = { subscriptionSettingsModel.ignoreAlerts.value = it },
        text = stringResource(R.string.add_calendar_alarms_ignore_title),
        summary = stringResource(R.string.add_calendar_alarms_ignore_description)
    )

    SwitchRow(
        checked = defaultAlarm != null,
        onCheckedChange = { onCheckedChange(it, subscriptionSettingsModel.defaultAlarmMinutes) },
        text = stringResource(R.string.add_calendar_alarms_default_title),
        summary = defaultAlarm?.let {
            val alarmPeriodText = PeriodFormat.wordBased().print(Minutes.minutes(it.toInt()))
            stringResource(R.string.add_calendar_alarms_default_description, alarmPeriodText)
        } ?: stringResource(R.string.add_calendar_alarms_default_none)
    )

    SwitchRow(
        checked = defaultAllDayAlarm != null,
        onCheckedChange = { onCheckedChange(it, subscriptionSettingsModel.defaultAllDayAlarmMinutes) },
        text = stringResource(R.string.add_calendar_alarms_default_all_day_title),
        summary = defaultAllDayAlarm?.let {
            val alarmPeriodText = PeriodFormat.wordBased().print(Minutes.minutes(it.toInt()))
            stringResource(R.string.add_calendar_alarms_default_description, alarmPeriodText)
        } ?: stringResource(R.string.add_calendar_alarms_default_none)
    )
}

@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.SubscriptionSettings(
    subscriptionSettingsModel: SubscriptionSettingsModel = viewModel()
) {
    SubscriptionSettings_TitleAndColor(subscriptionSettingsModel)

    SubscriptionSettings_Alarms(subscriptionSettingsModel)
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SubscriptionSettings_Preview() {
    Column {
        SubscriptionSettings()
    }
}
