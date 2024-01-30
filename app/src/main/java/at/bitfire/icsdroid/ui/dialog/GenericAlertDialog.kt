package at.bitfire.icsdroid.ui.dialog

import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable

/**
 * Provides a generic [AlertDialog] with some utilities.
 * @param confirmButton The first argument is the text of the button, the second one the callback.
 * @param dismissButton The first argument is the text of the button, the second one the callback.
 * @param title If any, the title to show in the dialog.
 * @param content Usually a [Text] element, though it can be whatever composable.
 * @param onDismissRequest Requested by the dialog when it should be closed.
 */
@Composable
fun GenericAlertDialog(
    confirmButton: Pair<String, () -> Unit>,
    dismissButton: Pair<String, () -> Unit>? = null,
    title: String? = null,
    content: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = title?.let {
            { Text(it) }
        },
        text = content,
        dismissButton = dismissButton?.let { (text, onClick) ->
            {
                TextButton(onClick = { onClick() }) { Text(text) }
            }
        },
        confirmButton = {
            val (text, onClick) = confirmButton
            TextButton(onClick = { onClick() }) { Text(text) }
        }
    )
}
