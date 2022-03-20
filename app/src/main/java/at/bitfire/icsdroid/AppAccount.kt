/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.provider.CalendarContract
import android.util.Log

object AppAccount {

    private const val DEFAULT_SYNC_INTERVAL = 24*3600L   // 1 day
    const val SYNC_INTERVAL_MANUALLY = -1L

    private const val PREF_ACCOUNT = "account"
    private const val KEY_SYNC_INTERVAL = "syncInterval"
    private const val KEY_USES_WORKMANAGER = "usesWorkManager"

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


    fun syncInterval(context: Context) =
        preferences(context).getLong(KEY_SYNC_INTERVAL, SYNC_INTERVAL_MANUALLY)

    fun syncInterval(context: Context, syncInterval: Long) {
        // don't use the sync framework anymore (legacy)
        ContentResolver.setSyncAutomatically(get(context), CalendarContract.AUTHORITY, false)

        // remember sync interval so that it can be checked/restored later
        preferences(context).edit()
                .putLong(KEY_SYNC_INTERVAL, syncInterval)
                .putBoolean(KEY_USES_WORKMANAGER, true)
                .apply()

        // set up periodic worker
        PeriodicSyncWorker.setInterval(context, if (syncInterval == SYNC_INTERVAL_MANUALLY) null else syncInterval)
    }


    /**
     * Checks whether the account sync interval is set as it should be.
     * If it is not, repair it (= set it to the remembered value).
     */
    fun checkSyncInterval(context: Context) {
        val prefs = preferences(context)
        if (!prefs.contains(KEY_USES_WORKMANAGER) && prefs.contains(KEY_SYNC_INTERVAL)) {
            // migrate from sync framework to WorkManager
            val rememberedSyncInterval = preferences(context).getLong(KEY_SYNC_INTERVAL, DEFAULT_SYNC_INTERVAL)
            Log.i(Constants.TAG, "Migrating from sync framework to WorkManager: periodic sync interval = $rememberedSyncInterval")
            syncInterval(context, rememberedSyncInterval)
        }
    }


    // helpers

    private fun preferences(context: Context) =
            context.getSharedPreferences(PREF_ACCOUNT, 0)

}
