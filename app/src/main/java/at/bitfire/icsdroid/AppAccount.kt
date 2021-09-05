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

    private const val DEFAULT_SYNC_INTERVAL = 24*3600L   // 1 day
    const val SYNC_INTERVAL_MANUALLY = -1L

    private const val PREF_ACCOUNT = "account"
    private const val KEY_SYNC_INTERVAL = "syncInterval"

    var account: Account? = null


    @Synchronized
    fun get(context: Context): Account {
        // use cached account, if available
        account?.let {
            return it
        }

        // get account
        val accountType = context.getString(R.string.account_type)

        val am = AccountManager.get(context)
        val existingAccount = am.getAccountsByType(accountType).firstOrNull()
        if (existingAccount != null) {
            // cache account so that checkSyncInterval etc. can use it
            account = existingAccount

            // check/repair sync interval
            checkSyncInterval(context)

            return existingAccount
        }

        Log.i(Constants.TAG, "Account not found, creating")
        val newAccount = Account(context.getString(R.string.account_name), accountType)
        if (am.addAccountExplicitly(newAccount, null, null)) {
            account = newAccount
            ContentResolver.setIsSyncable(newAccount, CalendarContract.AUTHORITY, 1)
            syncInterval(context, DEFAULT_SYNC_INTERVAL)
            return newAccount
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

        // remember sync interval so that it can be checked/restored later
        preferences(context).edit()
                .putLong(KEY_SYNC_INTERVAL, syncInterval)
                .apply()
    }


    /**
     * Checks whether the account sync interval is set as it should be.
     * If it is not, repair it (= set it to the remembered value).
     */
    fun checkSyncInterval(context: Context) {
        val prefs = preferences(context)
        if (prefs.contains(KEY_SYNC_INTERVAL)) {
            // there's a remembered sync interval
            val rememberedSyncInterval = preferences(context).getLong(KEY_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL)
            val currentSyncInterval = syncInterval(context)
            if (currentSyncInterval != rememberedSyncInterval) {
                Log.i(Constants.TAG, "Repairing sync interval from $currentSyncInterval -> $rememberedSyncInterval")
                syncInterval(context, rememberedSyncInterval)
            }
        }
    }


    // helpers

    private fun preferences(context: Context) =
            context.getSharedPreferences(PREF_ACCOUNT, 0)

}
