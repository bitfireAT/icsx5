/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.set_sync_interval.view.*

class SyncIntervalDialogFragment: DialogFragment() {

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        // read sync intervals from resources
        val syncIntervalSeconds = requireActivity().resources.getStringArray(R.array.set_sync_interval_seconds)
                .map { it.toLong() }

        val v = requireActivity().layoutInflater.inflate(R.layout.set_sync_interval, null)

        val currentSyncInterval = AppAccount.syncInterval(requireActivity())
        if (syncIntervalSeconds.contains(currentSyncInterval))
            v.sync_interval.setSelection(syncIntervalSeconds.indexOf(currentSyncInterval))

        builder .setView(v)
                .setPositiveButton(R.string.set_sync_interval_save) { _, _ ->
                    AppAccount.syncInterval(requireActivity(), syncIntervalSeconds[v.sync_interval.selectedItemPosition])
                }

        return builder.create()
    }

}