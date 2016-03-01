/*
 * Copyright (c) 2013 – 2016 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 */

package at.bitfire.icsdroid.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.R;

public class DonateDialogFragment extends DialogFragment {

    public DonateDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(R.drawable.ic_launcher)
                .setTitle(R.string.donate_title)
                .setMessage(R.string.donate_message)
                .setPositiveButton(R.string.donate_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Constants.donationUri));
                    }
                })
                .setNegativeButton(R.string.donate_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismiss();
                    }
                })
                .setCancelable(false)
                .create();
    }

}
