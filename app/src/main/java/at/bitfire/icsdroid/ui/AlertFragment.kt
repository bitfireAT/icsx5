package at.bitfire.icsdroid.ui

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlertFragment: DialogFragment() {

    companion object {

        const val ARG_MESSAGE = "message"

        fun create(message: String): AlertFragment {
            val frag = AlertFragment()
            val args = Bundle(1)
            args.putString(ARG_MESSAGE, message)
            frag.arguments = args
            return frag
        }

    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val message = args.getString(ARG_MESSAGE)
        val dialog = MaterialAlertDialogBuilder(requireActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ -> }
                .setNeutralButton(android.R.string.copy) { _, _ ->
                    val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText(null, message))
                }
        return dialog.create()
    }

}