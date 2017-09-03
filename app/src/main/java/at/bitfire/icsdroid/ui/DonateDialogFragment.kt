/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui;

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R

class DonateDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            AlertDialog.Builder(activity)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.donate_title)
                .setMessage(R.string.donate_message)
                .setPositiveButton(R.string.donate_now, { _, _ ->
                    startActivity(Intent(Intent.ACTION_VIEW, Constants.donationUri))
                })
                .setNegativeButton(R.string.donate_later, { _, _ ->
                    dismiss()
                })
                .setCancelable(false)
                .create()!!

}
