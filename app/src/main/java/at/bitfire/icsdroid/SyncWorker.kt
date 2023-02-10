/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

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
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
        context: Context,
        workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    companion object {

        /** The name of the worker. Tags the unique work. */
        const val NAME = "SyncWorker"

        /**
         * An input data for the Worker that tells whether the synchronization should be performed
         * without taking into account the current network condition.
         */
        private const val FORCE_RESYNC = "forceResync"

        /**
         * Enqueues a sync job for immediate execution. If the sync is forced,
         * the "requires network connection" constraint won't be set.
         *
         * @param context      required for managing work
         * @param force        *true* enqueues the sync regardless of the network state; *false* adds a [NetworkType.CONNECTED] constraint
         * @param forceResync  *true* ignores all locally stored data and fetched everything from the server again
         */
        fun run(context: Context, force: Boolean = false, forceResync: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(FORCE_RESYNC to forceResync))

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

    /** Interfaces with the Subscriptions table in the database */
    private lateinit var subscriptionsDao: SubscriptionsDao
    /** Interfaces with the Credentials table in the database */
    private lateinit var credentialsDao: CredentialsDao

    /** Stores all the loaded subscriptions */
    private lateinit var subscriptions: MutableList<Subscription>

    private var forceReSync: Boolean = false

    override suspend fun doWork(): Result {
        forceReSync = inputData.getBoolean(FORCE_RESYNC, false)

        return withContext(Dispatchers.Default) { performSync() }
    }

    private suspend fun performSync(): Result {
        Log.i(TAG, "Synchronizing (forceReSync=$forceReSync)")
        try {
            val database = AppDatabase.getInstance(applicationContext)
            // Get all the DAOs for interacting with the database
            subscriptionsDao = database.subscriptionsDao()
            credentialsDao = database.credentialsDao()

            // Get a list of all the subscriptions
            subscriptions = subscriptionsDao.getAll().toMutableList()

            // Migrate all old calendar-based subscriptions to database
            if (!migrateLegacyCalendars()) return Result.failure()

            // Migrate all credentials to the database
            migrateLegacyCredentials()

            // Sync subscriptions to calendars:
            // - Create/Update/Delete calendars according to the Subscriptions
            // - Sync each subscription/calendar with server/file.
            syncCalendars()
        } catch (e: CalendarStorageException) {
            Log.e(TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thread interrupted", e)
        }

        return Result.success()
    }

    /**
     * Migrates all the legacy calendar-based subscriptions to the database. Performs these steps:
     * 1. Searches for all the calendars created
     * 2. Checks that those calendars have a matching [Subscription] in the database.
     * 3. If there's no matching [Subscription], create it.
     * @return `true` if the migration was completed successfully, `false` otherwise (No provider
     * is available for [getApplicationContext]).
     */
    private fun migrateLegacyCalendars(): Boolean {
        val account = AppAccount.get(applicationContext)

        // Get a provider from the application context, or return failure
        val provider = DatabaseAndroidInterface.getProvider(applicationContext) ?: return false
        // If there's a provider available, get all the calendars available in the system
        val calendars = AndroidCalendar.find(
            account,
            provider,
            LocalCalendar.Factory,
            null,
            null,
        )

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

        return true
    }

    /**
     * Migrates all the old credentials (from shared preferences) to the database. Steps:
     * 1. For each subscription, get its matching calendar.
     * 2. Get the credentials stored in [CalendarCredentials].
     * 3. If there are credentials stored, add them to the database.
     * 4. Remove all the stored credentials from [CalendarCredentials].
     */
    @Suppress("DEPRECATION")
    private fun migrateLegacyCredentials() {
        val oldCredentials = CalendarCredentials(applicationContext)
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
    }

    /**
     * Takes all the [subscriptions], and runs [ProcessEventsTask] with them if [Subscription.isSynced]
     * is true.
     */
    private suspend fun syncCalendars() {
        subscriptions
            .filter { it.isSynced }
            .forEach { ProcessEventsTask(applicationContext, it, forceReSync).sync() }
    }

}
