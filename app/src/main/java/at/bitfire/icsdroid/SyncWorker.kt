/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.work.*
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.DatabaseAndroidInterface
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
        context: Context,
        workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    companion object {

        const val NAME = "SyncWorker"

        const val FORCE_RESYNC = "forceResync"

        /**
         * Gives the type of the account to synchronized. Must be defined together with
         * [ACCOUNT_NAME], allows selecting the account to be synchronized.
         */
        const val ACCOUNT_TYPE = "accountType"

        /**
         * Gives the name of the account to synchronized. Must be defined together with
         * [ACCOUNT_NAME], allows selecting the account to be synchronized.
         */
        const val ACCOUNT_NAME = "accountName"


        /**
         * Enqueues a sync job for immediate execution. If the sync is forced,
         * the "requires network connection" constraint won't be set.
         *
         * @param context      required for managing work
         * @param force        *true* enqueues the sync regardless of the network state; *false* adds a [NetworkType.CONNECTED] constraint
         * @param forceResync  *true* ignores all locally stored data and fetched everything from the server again
         * @param account      _Used for testing._ The account to be used with the worker. Defaults
         * to [AppAccount.get].
         */
        fun run(
            context: Context,
            force: Boolean = false,
            forceResync: Boolean = false,
            account: Account? = null,
        ) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(
                    workDataOf(
                        *arrayListOf<Pair<String, Any?>>(FORCE_RESYNC to forceResync).apply {
                            if (account != null) {
                                add(ACCOUNT_NAME to account.name)
                                add(ACCOUNT_TYPE to account.type)
                            }
                        }.toTypedArray()
                    )
                )

            val policy: ExistingWorkPolicy = if (force) {
                Log.i(TAG, "Manual sync, ignoring network condition")

                // overwrite existing syncs (which may have unwanted constraints)
                ExistingWorkPolicy.REPLACE
            } else {
                // regular sync, requires network
                request.setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())

                // don't overwrite previous syncs (whether regular or manual)
                ExistingWorkPolicy.KEEP
            }

            WorkManager.getInstance(context)
                    .beginUniqueWork(NAME, policy, request.build())
                    .enqueue()
        }

        fun liveStatus(context: Context) =
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(NAME)

    }


    @SuppressLint("Recycle")
    override suspend fun doWork(): Result {
        val forceResync = inputData.getBoolean(FORCE_RESYNC, false)

        // Get a custom account, or the default one
        val account = if (
        // If input data has an account name
            inputData.hasKeyWithValueOfType<String>(ACCOUNT_NAME) &&
            // And an account type
            inputData.hasKeyWithValueOfType<String>(ACCOUNT_TYPE)
        ) {
            // Initialize a new account with the given parameters
            Account(inputData.getString(ACCOUNT_NAME), inputData.getString(ACCOUNT_TYPE))
        } else {
            // Otherwise get the AppAccount
            AppAccount.get(applicationContext)
        }

        return withContext(Dispatchers.Default) {
            performSync(account, forceResync)
        }
    }

    private suspend fun performSync(account: Account, forceResync: Boolean): Result {
        Log.i(TAG, "Synchronizing ${account.name} (forceResync=$forceResync)")
        try {
            val database = AppDatabase.getInstance(applicationContext)
            // Get the subscriptions dao for interacting with the database.
            val subscriptionsDao = database.subscriptionsDao()
            // Get a list of all the subscriptions from the database
            val subscriptions = subscriptionsDao.getAll().toMutableList()

            // Get a provider from the application context, or return failure
            val provider = DatabaseAndroidInterface.getProvider(applicationContext) ?: return Result.failure()
            // If there's a provider available, get all the calendars available in the system
            val calendars = AndroidCalendar.find(
                account,
                provider,
                LocalCalendar.Factory,
                null,
                null,
            )

            // Check that all the calendars have a matching subscription
            for (calendar in calendars) {
                val id = calendar.id
                val match = subscriptions.find { subscription -> subscription.id == id }
                // If there's already a calendar matching the subscription, continue
                if (match != null) continue
                try {
                    // Otherwise, create a subscription for the calendar
                    val newSubscription = Subscription.fromCalendar(calendar)
                    // Add it to the database
                    subscriptionsDao.add(newSubscription)
                    // And add it to `subscriptions` so it gets processed now.
                    subscriptions.add(newSubscription)
                    Log.i(TAG, "The calendar #${calendar.id} didn't have a matching subscription. Just created it.")
                } catch (e: Exception) {
                    Log.e(TAG, "Could not create subscription from calendar. Migration failed.", e)
                    continue
                }
            }

            // Migrate all credentials to the database
            val credentialsDao = database.credentialsDao()
            @Suppress("DEPRECATION") val oldCredentials = CalendarCredentials(applicationContext)
            for (subscription in subscriptions) {
                val databaseAndroidInterface = DatabaseAndroidInterface(applicationContext, subscription)
                val calendar = databaseAndroidInterface.getCalendar()
                calendar
                    // Get the credentials that might be stored for the subscription
                    .let { oldCredentials.get(it) }
                    // Take only if there's an username and password
                    .takeIf { (u, p) -> u != null && p != null }
                    // Convert the username and password into Credential
                    ?.let { Credential(subscription.id, it.first!!, it.second!!) }
                    // Store the credential in the database
                    ?.let { credentialsDao.put(it.subscriptionId, it.username, it.password) }
                    // Remove the credential from shared preferences
                    ?.also { oldCredentials.put(calendar, null, null) }
            }

            // Process each subscription
            subscriptions
                .filter { it.isSynced }
                .forEach { ProcessEventsTask(applicationContext, it, forceResync).sync() }

        } catch (e: CalendarStorageException) {
            Log.e(TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thread interrupted", e)
        }

        return Result.success()
    }

}
