/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.SetSyncIntervalBinding

@Deprecated("Migrate to Jetpack Compose")
class SyncIntervalDialogFragment: DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        // read sync intervals from resources
        val syncIntervalSeconds = requireActivity().resources.getStringArray(R.array.set_sync_interval_seconds)
                .map { it.toLong() }

        val binding = SetSyncIntervalBinding.inflate(requireActivity().layoutInflater)

        val currentSyncInterval = AppAccount.syncInterval(requireActivity())
        if (syncIntervalSeconds.contains(currentSyncInterval))
            binding.syncInterval.setSelection(syncIntervalSeconds.indexOf(currentSyncInterval))

        builder .setView(binding.root)
                .setPositiveButton(R.string.set_sync_interval_save) { _, _ ->
                    AppAccount.syncInterval(requireActivity(), syncIntervalSeconds[binding.syncInterval.selectedItemPosition])
                }

        return builder.create()
    }

}