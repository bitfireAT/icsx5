/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Dialog
import android.os.Bundle
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.R
import java.io.PrintWriter
import java.io.StringWriter

class AlertFragment: DialogFragment() {

    companion object {

        const val ARG_MESSAGE = "message"
        const val ARG_THROWABLE = "throwable"

        fun create(message: String, throwable: Throwable? = null): AlertFragment {
            val frag = AlertFragment()
            frag.arguments = bundleOf(
                ARG_MESSAGE to message,
                ARG_THROWABLE to throwable
            )
            return frag
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val message = args.getString(ARG_MESSAGE).orEmpty()
        val throwable = args.getSerializable(ARG_THROWABLE) as? Throwable

        val dialog = Dialog(requireContext())
        dialog.setContentView(
            ComposeView(requireContext()).apply {
                setContent {
                    AlertFragmentDialog(message, throwable) { dismiss() }
                }
            }
        )

        return dialog
    }

}

@Composable
fun AlertFragmentDialog(
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
