/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import at.bitfire.icsdroid.R

class DonateDialogFragment: DialogFragment() {

    companion object {
        const val PREF_NEXT_REMINDER = "nextDonationReminder"

        val donationUri = Uri.parse("https://icsx5.bitfire.at/donate/?pk_campaign=icsx5-app")!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
            AlertDialog.Builder(activity!!)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.donate_title)
                .setMessage(R.string.donate_message)
                .setPositiveButton(R.string.donate_now) { _, _ ->
                    requireActivity().getPreferences(0).edit()
                            .putLong(PREF_NEXT_REMINDER, System.currentTimeMillis() + 60*86400000L)
                            .apply()
                    startActivity(Intent(Intent.ACTION_VIEW, donationUri))
                }
                .setNegativeButton(R.string.donate_later) { _, _ ->
                    requireActivity().getPreferences(0).edit()
                            .putLong(PREF_NEXT_REMINDER, System.currentTimeMillis() + 14*86400000L)
                            .apply()
                    dismiss()
                }
                .setCancelable(false)
                .create()!!


    class Factory: StartupFragment {

        override fun initialize(activity: AppCompatActivity) {
            if (activity.getPreferences(0).getLong(PREF_NEXT_REMINDER, 0) < System.currentTimeMillis())
                DonateDialogFragment().show(activity.supportFragmentManager, "donate")
        }

    }

}
