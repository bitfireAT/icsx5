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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.PeriodicSync;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;

public class AppAccount {
    public static final long SYNC_INTERVAL_MANUALLY = -1;

    public static final Account account = new Account("ICSdroid", "at.bitfire.icsdroid");


    public static void makeAvailable(Context context) {
        AccountManager am = AccountManager.get(context);
        if (am.getAccountsByType(account.type).length == 0) {
            Log.i(Constants.TAG, "Account not found, creating");
            am.addAccountExplicitly(AppAccount.account, null, null);
            ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
        }
    }

    public static boolean isSyncActive() {
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
            Log.i(Constants.TAG, "Disabling automatic synchronization");
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, false);
        } else {
            Log.i(Constants.TAG, "Setting automatic synchronization with interval of " + syncInterval + " seconds");
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true);
            ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, new Bundle(), syncInterval);
        }
    }
}
