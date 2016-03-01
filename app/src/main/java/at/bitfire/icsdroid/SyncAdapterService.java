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

package at.bitfire.icsdroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import lombok.Synchronized;

public class SyncAdapterService extends Service {

    private static SyncAdapter syncAdapter;

    @Override
    @Synchronized
    public void onCreate() {
        if (syncAdapter == null)
            syncAdapter = new SyncAdapter(this);
    }

    @Override
    @Synchronized
    public void onDestroy() {
        syncAdapter = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }

}
