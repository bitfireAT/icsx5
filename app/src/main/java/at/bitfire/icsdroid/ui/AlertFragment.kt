/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.app.ShareCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.utils.getSerializableCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.PrintWriter
import java.io.StringWriter

class AlertFragment : DialogFragment() {

    companion object {

        const val ARG_MESSAGE = "message"
        const val ARG_THROWABLE = "throwable"

        fun create(message: String, throwable: Throwable? = null): AlertFragment = AlertFragment().apply {
            arguments = bundleOf(
                ARG_MESSAGE to message,
                ARG_THROWABLE to throwable,
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val message = args.getString(ARG_MESSAGE).orEmpty()
        val dialog = MaterialAlertDialogBuilder(requireActivity())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ -> }
            .setNeutralButton(R.string.alert_share_details) { _, _ ->
                val details = StringWriter()
                details.append(message)

                (args.getSerializableCompat(ARG_THROWABLE, Throwable::class))?.let { ex ->
                    details.append("\n\n")
                    ex.printStackTrace(PrintWriter(details))
                }

                val share = ShareCompat.IntentBuilder(requireContext())
                    .setType("text/plain")
                    .setText(details.toString())
                    .createChooserIntent()
                startActivity(share)
            }
        return dialog.create()
    }

}