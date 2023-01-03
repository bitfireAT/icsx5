/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.work.*
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.db.sync.SubscriptionAndroidCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {

        const val NAME = "SyncWorker"

        const val FORCE_RESYNC = "forceResync"

        /**
         * The maximum number of attempts to make until considering the server as "unreachable".
         */
        private const val MAX_ATTEMPTS = 5

        /**
         * The amount of time (in seconds) to wait once the conditions are met, before launching the work.
         */
        private const val INITIAL_DELAY = 10L


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
                // Add an initial delay of 20 seconds to allow the network connection to boot up
                .setInitialDelay(INITIAL_DELAY, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .setInputData(workDataOf(FORCE_RESYNC to forceResync))

            val policy: ExistingWorkPolicy = if (force) {
                Log.i(TAG, "Manual sync, ignoring network condition")

                // overwrite existing syncs (which may have unwanted constraints)
                ExistingWorkPolicy.REPLACE
            } else {
                // regular sync, requires network
                request.setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )

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

    override suspend fun doWork(): Result {
        val forceResync = inputData.getBoolean(FORCE_RESYNC, false)

        return withContext(Dispatchers.Default) {
            performSync(AppAccount.get(applicationContext), forceResync)
        }
    }

    /**
     * Fecthes all the subscriptions from the database, and runs [ProcessEventsTask] on each of them.
     * @param account The owner account of the subscriptions.
     * @param forceResync Enforces that the calendar is fetched and all events are fully processed (useful when subscription settings have been changed).
     */
    private suspend fun performSync(account: Account, forceResync: Boolean): Result {
        Log.i(TAG, "Synchronizing ${account.name} (forceResync=$forceResync)")
        try {
            // Get the subscriptions dao for interacting with the database.
            val dao = AppDatabase.getInstance(applicationContext)
                .subscriptionsDao()
            // Get a list of all the subscriptions from the database
            val subscriptions = dao.getAll().toMutableList()

            // Get a provider from the application context, or return failure
            val provider = Subscription.getProvider(applicationContext) ?: return Result.failure()
            // If there's a provider available, get all the calendars available in the system
            val calendars = AndroidCalendar.find(
                account,
                provider,
                SubscriptionAndroidCalendar.Factory(),
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
                    dao.add(newSubscription)
                    // And add it to `subscriptions` so it gets processed now.
                    subscriptions.add(newSubscription)
                } catch (e: Exception) {
                    Log.e(TAG, "Could not create subscription from calendar. Migration failed.", e)
                    continue
                }
            }

            // Process each subscription
            subscriptions
                .filter { it.isSynced }
                .forEach { ProcessEventsTask(applicationContext, it, forceResync).sync() }

            return Result.success()
        } catch (e: CalendarStorageException) {
            Log.e(TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thread interrupted", e)
        }

        return if (runAttemptCount >= MAX_ATTEMPTS)
            Result.failure()
        else
            Result.retry()
    }

}
