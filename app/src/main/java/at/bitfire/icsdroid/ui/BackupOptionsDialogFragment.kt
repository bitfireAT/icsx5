package at.bitfire.icsdroid.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.R

class BackupOptionsDialogFragment(
    /** Will get called when the user requests to import a backup. */
    private val onImportRequested: () -> Unit,
    /** Will get called when the user requests to export a backup. */
    private val onExportRequested: () -> Unit,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(activity)
            .setTitle(R.string.backup_options_title)
            .setMessage(R.string.backup_options_alert)
            .setPositiveButton(R.string.backup_options_import) { dialog, _ ->
                onImportRequested()
                dialog.dismiss()
            }
            .setNeutralButton(R.string.backup_options_export) { dialog, _ ->
                onExportRequested()
                dialog.dismiss()
            }
            .create()
}
