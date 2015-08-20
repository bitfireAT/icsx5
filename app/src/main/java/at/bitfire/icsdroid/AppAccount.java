package at.bitfire.icsdroid;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;

import net.fortuna.ical4j.model.Content;

public class AppAccount {
    private static final String TAG = "ICSdroid.AppAccount";

    public static final long SYNC_INTERVAL_MANUALLY = -1;

    public static final Account account = new Account("ICSdroid", "at.bitfire.icsdroid");


    public static void makeAvailable(Context context) {
        AccountManager am = AccountManager.get(context);
        if (am.getAccountsByType(account.type).length == 0) {
            Log.i(TAG, "Account not found, creating");
            am.addAccountExplicitly(AppAccount.account, null, null);
            ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
        }
    }

    public static boolean isSyncActive(Context context) {
        return ContentResolver.isSyncActive(AppAccount.account, CalendarContract.AUTHORITY);
    }

    public static long getSyncInterval(Context context) {
        makeAvailable(context);

        long syncInterval = SYNC_INTERVAL_MANUALLY;
        if (ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY))
            for (PeriodicSync sync : ContentResolver.getPeriodicSyncs(account, CalendarContract.AUTHORITY))
                syncInterval = sync.period;
        return syncInterval;
    }

    public static void setSyncInterval(long syncInterval) {
        if (syncInterval == -1) {
            Log.i(TAG, "Disabling automatic synchronization");
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, false);
        } else {
            Log.i(TAG, "Setting automatic synchronization with interval of " + syncInterval + " seconds");
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), syncInterval);
        }
    }
}
