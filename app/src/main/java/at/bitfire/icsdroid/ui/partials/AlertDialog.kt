/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.partials

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ShareCompat
import at.bitfire.icsdroid.R
import java.io.PrintWriter
import java.io.StringWriter

@Composable
fun AlertDialog(
    message: String,
    throwable: Throwable? = null,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    val details = StringWriter()
                    details.append(message)

                    if (throwable != null) {
                        details.append("\n\n")
                        throwable.printStackTrace(PrintWriter(details))
                    }

                    val share = ShareCompat.IntentBuilder(context)
                        .setType("text/plain")
                        .setText(details.toString())
                        .createChooserIntent()
                    context.startActivity(share)
                }
            ) {
                Text(stringResource(R.string.alert_share_details))
            }
        }
    )
}
