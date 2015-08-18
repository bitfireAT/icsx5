package at.bitfire.icsdroid;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import lombok.Synchronized;

public class SyncAdapterService extends Service {

    private static SyncAdapter syncAdapter;

    @Override
    @Synchronized
    public void onCreate() {
        if (syncAdapter == null)
            syncAdapter = new SyncAdapter(getApplicationContext());
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
