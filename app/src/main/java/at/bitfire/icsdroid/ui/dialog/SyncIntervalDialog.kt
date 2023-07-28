package at.bitfire.icsdroid.ui.dialog

import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.R

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SyncIntervalDialog(
    onSetSyncInterval: (Long) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    val syncIntervalValues = stringArrayResource(R.array.set_sync_interval_seconds)
    val syncIntervalOptions = stringArrayResource(R.array.set_sync_interval_names)
        .mapIndexed { i, s -> syncIntervalValues[i].toLong() to s }
        .toMap()

    var syncInterval by remember { mutableStateOf(AppAccount.syncInterval(context)) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(R.string.set_sync_interval_title),
                style = MaterialTheme.typography.subtitle1
            )
        },
        text = {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = syncIntervalOptions.getValue(syncInterval),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    syncIntervalOptions.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                syncInterval = option.key
                                expanded = false
                            }
                        ) {
                            Text(option.value)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSetSyncInterval(syncInterval) }
            ) {
                Text(
                    text = stringResource(R.string.set_sync_interval_save).uppercase()
                )
            }
        }
    )
}

@Preview
@Composable
fun SyncIntervalDialog_Preview() {
    SyncIntervalDialog(
        onSetSyncInterval = {},
        onDismissRequest = {}
    )
}
