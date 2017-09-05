/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log

object AppAccount {

    val SYNC_INTERVAL_MANUALLY = -1L

    val account = Account("ICSdroid", "at.bitfire.icsdroid")


    fun makeAvailable(context: Context) {
        val am = AccountManager.get(context)
        if (am.getAccountsByType(account.type).isEmpty()) {
            Log.i(Constants.TAG, "Account not found, creating")
            am.addAccountExplicitly(AppAccount.account, null, null)
            ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1)
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true)
        }
    }

    fun isSyncActive() =
            ContentResolver.isSyncActive(AppAccount.account, CalendarContract.AUTHORITY)

    fun getSyncInterval(context: Context): Long {
        makeAvailable(context)

        var syncInterval = SYNC_INTERVAL_MANUALLY
        if (ContentResolver.getSyncAutomatically(account, CalendarContract.AUTHORITY))
            for (sync in ContentResolver.getPeriodicSyncs(account, CalendarContract.AUTHORITY))
                syncInterval = sync.period
        return syncInterval
    }

    fun setSyncInterval(syncInterval: Long) {
        if (syncInterval == SYNC_INTERVAL_MANUALLY) {
            Log.i(Constants.TAG, "Disabling automatic synchronization")
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, false)
        } else {
            Log.i(Constants.TAG, "Setting automatic synchronization with interval of $syncInterval seconds")
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true)
            ContentResolver.addPeriodicSync(account, CalendarContract.AUTHORITY, Bundle(), syncInterval)
        }
    }

}
