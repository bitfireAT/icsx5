package at.bitfire.icsdroid.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import at.bitfire.icsdroid.R

@Composable
fun DeleteConfirmationDialog(onDismissRequest: () -> Unit, onDeleteRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDeleteRequest) {
                Text(stringResource(R.string.edit_calendar_delete))
            }
        },
        text = { Text(stringResource(R.string.edit_calendar_really_delete)) },
    )
}
