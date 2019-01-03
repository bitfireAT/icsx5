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

    const val SYNC_INTERVAL_MANUALLY = -1L

    fun get(context: Context): Account {
        val accountType = context.getString(R.string.account_type)

        val am = AccountManager.get(context)
        val existingAccount = am.getAccountsByType(accountType).firstOrNull()
        if (existingAccount != null)
            return existingAccount

        Log.i(Constants.TAG, "Account not found, creating")
        val account = Account(context.getString(R.string.account_name), accountType)
        if (am.addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1)
            ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true)
            return account
        }

        throw IllegalStateException("Couldn't create app account")
    }


    fun syncInterval(context: Context): Long {
        var syncInterval = SYNC_INTERVAL_MANUALLY
        if (ContentResolver.getSyncAutomatically(get(context), CalendarContract.AUTHORITY))
            for (sync in ContentResolver.getPeriodicSyncs(get(context), CalendarContract.AUTHORITY))
                syncInterval = sync.period
        return syncInterval
    }

    fun syncInterval(context: Context, syncInterval: Long) {
        if (syncInterval == SYNC_INTERVAL_MANUALLY) {
            Log.i(Constants.TAG, "Disabling automatic synchronization")
            ContentResolver.setSyncAutomatically(get(context), CalendarContract.AUTHORITY, false)
        } else {
            Log.i(Constants.TAG, "Setting automatic synchronization with interval of $syncInterval seconds")
            ContentResolver.setSyncAutomatically(get(context), CalendarContract.AUTHORITY, true)
            ContentResolver.addPeriodicSync(get(context), CalendarContract.AUTHORITY, Bundle(), syncInterval)
        }
    }

}
