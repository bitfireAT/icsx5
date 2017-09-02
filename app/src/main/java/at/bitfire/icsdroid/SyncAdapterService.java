/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
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
