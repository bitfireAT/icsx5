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
import android.app.DialogFragment
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.R
import kotlinx.android.synthetic.main.set_sync_interval.view.*
import java.util.*

class SyncIntervalDialogFragment: DialogFragment(), AdapterView.OnItemSelectedListener {

    private val syncIntervalSeconds = LinkedList<Long>()
    private var syncInterval: Long = 0

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        // read sync intervals from resources
        activity.resources.getStringArray(R.array.set_sync_interval_seconds)
                .mapTo(syncIntervalSeconds) { it.toLong() }

        val v = activity.layoutInflater.inflate(R.layout.set_sync_interval, null)

        val currentSyncInterval = AppAccount.getSyncInterval(activity)
        if (syncIntervalSeconds.contains(currentSyncInterval))
            v.sync_interval.setSelection(syncIntervalSeconds.indexOf(currentSyncInterval))
        v.sync_interval.onItemSelectedListener = this

        builder .setView(v)
                .setPositiveButton(R.string.set_sync_interval_save, { _, _ ->
                    AppAccount.setSyncInterval(syncInterval)
                })

        return builder.create()
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        syncInterval = syncIntervalSeconds.get(position)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {
        syncInterval = AppAccount.SYNC_INTERVAL_MANUALLY
    }

}