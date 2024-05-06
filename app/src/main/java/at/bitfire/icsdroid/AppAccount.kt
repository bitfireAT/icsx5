/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.provider.CalendarContract
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object AppAccount {

    const val DEFAULT_SYNC_INTERVAL = 24*3600L   // 1 day
    const val SYNC_INTERVAL_MANUALLY = -1L

    private const val PREF_ACCOUNT = "account"
    private const val KEY_SYNC_INTERVAL = "syncInterval"

    private var account: Account? = null


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
            account = existingAccount
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


    fun syncInterval(context: Context) =
        preferences(context).getLong(KEY_SYNC_INTERVAL, SYNC_INTERVAL_MANUALLY)

    fun syncIntervalFlow(context: Context) = callbackFlow {
        val preferences = preferences(context)
        val listener = OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SYNC_INTERVAL) {
                trySend(preferences.getLong(KEY_SYNC_INTERVAL, SYNC_INTERVAL_MANUALLY))
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    fun syncInterval(context: Context, syncInterval: Long) {
        // don't use the sync framework anymore (legacy)
        ContentResolver.setSyncAutomatically(get(context), CalendarContract.AUTHORITY, false)

        // remember sync interval so that it can be checked/restored later
        preferences(context).edit()
                .putLong(KEY_SYNC_INTERVAL, syncInterval)
                .apply()

        // set up periodic worker
        PeriodicSyncWorker.setInterval(
            context,
            if (syncInterval == SYNC_INTERVAL_MANUALLY)
                null
            else
                syncInterval
        )
    }


    // helpers

    private fun preferences(context: Context) =
            context.getSharedPreferences(PREF_ACCOUNT, 0)

}
