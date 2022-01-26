/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.app.ShareCompat
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.PrintWriter
import java.io.StringWriter

class AlertFragment: DialogFragment() {

    companion object {

        const val ARG_MESSAGE = "message"
        const val ARG_THROWABLE = "throwable"

        fun create(message: String, throwable: Throwable? = null): AlertFragment {
            val frag = AlertFragment()
            val args = Bundle(2)
            args.putString(ARG_MESSAGE, message)
            args.putSerializable(ARG_THROWABLE, throwable)
            frag.arguments = args
            return frag
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

                    (args.getSerializable(ARG_THROWABLE) as? Throwable)?.let { ex ->
                        details.append("\n\n")
                        ex.printStackTrace(PrintWriter(details))
                    }

                    val share = ShareCompat.IntentBuilder.from(requireActivity())
                            .setType("text/plain")
                            .setText(details.toString())
                            .createChooserIntent()
                    startActivity(share)
                }
        return dialog.create()
    }

}