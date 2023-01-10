package at.bitfire.icsdroid.ui.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.R

@Composable
@ExperimentalMaterial3Api
fun SyncIntervalDialog(onDismissRequested: () -> Unit) {
    val context = LocalContext.current

    var expanded by remember { mutableStateOf(false) }
    var selection by remember { mutableStateOf(0) }

    val options = context.resources.getStringArray(R.array.set_sync_interval_names)
    val syncIntervalSeconds = context.resources.getStringArray(R.array.set_sync_interval_seconds)
        .map { it.toLong() }

    AlertDialog(
        onDismissRequest = onDismissRequested,
        title = { Text(stringResource(R.string.set_sync_interval_title)) },
        text = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = options[selection],
                    onValueChange = {},
                    modifier = Modifier.menuAnchor(),
                    readOnly = true,
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    options.forEachIndexed { index, value ->
                        DropdownMenuItem(
                            text = { Text(value) },
                            onClick = { selection = index; expanded = false },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    AppAccount.syncInterval(context, syncIntervalSeconds[selection])
                    onDismissRequested()
                },
            ) {
                Text(stringResource(R.string.set_sync_interval_save))
            }
        },
    )
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SyncIntervalDialogPreview() {
    SyncIntervalDialog { }
}
