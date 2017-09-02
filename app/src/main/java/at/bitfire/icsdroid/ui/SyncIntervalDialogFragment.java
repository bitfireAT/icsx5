/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import java.util.LinkedList;
import java.util.List;

import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;

public class SyncIntervalDialogFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    private List<Long> syncIntervalSeconds = new LinkedList<>();
    private long syncInterval;

    public static SyncIntervalDialogFragment newInstance() {
        return new SyncIntervalDialogFragment();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // read sync intervals from resources
        for (String syncInterval : getActivity().getResources().getStringArray(R.array.set_sync_interval_seconds))
            syncIntervalSeconds.add(Long.valueOf(syncInterval));

        @SuppressLint("InflateParams") View v = getActivity().getLayoutInflater().inflate(R.layout.set_sync_interval, null);
        Spinner spinnerInterval = (Spinner)v.findViewById(R.id.sync_interval);
        long currentSyncInterval = AppAccount.getSyncInterval(getActivity());
        if (syncIntervalSeconds.contains(currentSyncInterval))
            spinnerInterval.setSelection(syncIntervalSeconds.indexOf(currentSyncInterval));
        spinnerInterval.setOnItemSelectedListener(this);

        builder .setView(v)
                .setPositiveButton(R.string.set_sync_interval_save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppAccount.setSyncInterval(syncInterval);
                    }
                });

        return builder.create();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        syncInterval = syncIntervalSeconds.get(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        syncInterval = AppAccount.SYNC_INTERVAL_MANUALLY;
    }
}
