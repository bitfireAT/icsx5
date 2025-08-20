/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.ui.partials.ColorPickerDialog
import at.bitfire.icsdroid.ui.partials.SwitchSetting
import at.bitfire.icsdroid.ui.theme.AppTheme

@Composable
fun SubscriptionSettingsComposable(
    url: String?,
    title: String?,
    titleChanged: (String) -> Unit,
    color: Int?,
    colorChanged: (Int) -> Unit,
    customUserAgent: String?,
    customUserAgentChanged: (String) -> Unit,
    ignoreAlerts: Boolean,
    ignoreAlertsChanged: (Boolean) -> Unit,
    defaultAlarmMinutes: Long?,
    defaultAlarmMinutesChanged: (String) -> Unit,
    defaultAllDayAlarmMinutes: Long?,
    defaultAllDayAlarmMinutesChanged: (String) -> Unit,
    ignoreDescription: Boolean,
    onIgnoreDescriptionChanged: (Boolean) -> Unit,
    isCreating: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {

        // Title
        Text(
            text = stringResource(R.string.add_calendar_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        // Name and color card
        Card (
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.weight(5f)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        text = url ?: "",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    TextField(
                        value = title ?: "",
                        onValueChange = titleChanged,
                        label = { Text(stringResource(R.string.add_calendar_title_hint)) },
                        singleLine = true,
                        enabled = !isCreating
                    )
                }
                var changeColorDialogOpen by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { changeColorDialogOpen = true },
                    modifier = Modifier
                        .weight(1f)
                        .size(48.dp)
                        .padding(start = 8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Circle,
                        contentDescription = stringResource(R.string.add_calendar_pick_color),
                        tint = color?.let { Color(it) } ?: Color.Unspecified,
                        modifier = Modifier
                            .size(48.dp)
                    )
                }
                // Color picker dialog
                if (changeColorDialogOpen)
                    ColorPickerDialog(
                        initialColor = color ?: LocalCalendar.DEFAULT_COLOR,
                        onSelectColor = colorChanged,
                        onDialogDismissed = { changeColorDialogOpen = false }
                    )
            }
        }

        Spacer(modifier = Modifier.padding(12.dp))

        // Alarms
        Text(
            text = stringResource(R.string.add_calendar_alarms_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        // Ignore existing alarms
        SwitchSetting(
            title = stringResource(R.string.add_calendar_alarms_ignore_title),
            description = stringResource(R.string.add_calendar_alarms_ignore_description),
            checked = ignoreAlerts,
            onCheckedChange = ignoreAlertsChanged
        )

        Spacer(modifier = Modifier.padding(12.dp))

        // Default Alarm
        Text(
            text = stringResource(R.string.default_alarm_dialog_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.default_alarm_dialog_message),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = (defaultAlarmMinutes ?: "").toString(),
            onValueChange = defaultAlarmMinutesChanged,
            label = { Text(stringResource(R.string.default_alarm_dialog_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        )

        Spacer(modifier = Modifier.padding(12.dp))

        // Default Alarm (All Day Events)
        Text(
            text = stringResource(R.string.add_calendar_alarms_default_all_day_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.default_alarm_dialog_message),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = (defaultAllDayAlarmMinutes ?: "").toString(),
            onValueChange = defaultAllDayAlarmMinutesChanged,
            label = { Text(stringResource(R.string.default_alarm_dialog_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        )

        Spacer(modifier = Modifier.padding(12.dp))

        // Advanced

        Text(
            text = stringResource(R.string.add_calendar_advanced_title),
            style = MaterialTheme.typography.headlineSmall,
        )

        SwitchSetting(
            title = stringResource(R.string.add_calendar_description_title),
            description = stringResource(R.string.add_calendar_description_summary),
            checked = ignoreDescription,
            onCheckedChange = onIgnoreDescriptionChanged
        )

        // Custom User Agent
        Text(
            text = stringResource(R.string.add_calendar_custom_user_agent_title),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.add_calendar_custom_user_agent_description),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = customUserAgent ?: "",
            onValueChange = customUserAgentChanged,
            label = { Text(stringResource(R.string.add_calendar_custom_user_agent_title)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCreating
        )
    }
}

@Preview
@Composable
fun SubscriptionSettingsComposable_Preview() {
    AppTheme {
        SubscriptionSettingsComposable(
            url = "url",
            title = "title",
            titleChanged = {},
            color = 0,
            colorChanged = {},
            customUserAgent = "customUserAgent",
            customUserAgentChanged = {},
            ignoreAlerts = true,
            ignoreAlertsChanged = {},
            defaultAlarmMinutes = 20L,
            defaultAlarmMinutesChanged = {},
            defaultAllDayAlarmMinutes = 10L,
            defaultAllDayAlarmMinutesChanged = {},
            ignoreDescription = false,
            onIgnoreDescriptionChanged = {},
            isCreating = true
        )
    }
}