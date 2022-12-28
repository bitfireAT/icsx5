/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.content.Context
import android.util.Log
import androidx.work.*
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.db.AppDatabase
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
         * @since 20221212
         */
        private const val MAX_ATTEMPTS = 5

        /**
         * The amount of time (in seconds) to wait once the conditions are met, before launching the work.
         * @since 20221214
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
                Log.i(Constants.TAG, "Manual sync, ignoring network condition")

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
     * @since 20221228
     * @param account The owner account of the subscriptions.
     * @param forceResync Enforces that the calendar is fetched and all events are fully processed (useful when subscription settings have been changed).
     */
    private suspend fun performSync(account: Account, forceResync: Boolean): Result {
        Log.i(Constants.TAG, "Synchronizing ${account.name} (forceResync=$forceResync)")
        try {
            AppDatabase.getInstance(applicationContext)
                .subscriptionsDao()
                .getAll()
                .filter { it.isSynced }
                .forEach { ProcessEventsTask(applicationContext, it, forceResync).sync() }

            // TODO: Remove all calendars from system that have been removed from database

            return Result.success()
        } catch (e: CalendarStorageException) {
            Log.e(Constants.TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Thread interrupted", e)
        }

        return if (runAttemptCount >= MAX_ATTEMPTS)
            Result.failure()
        else
            Result.retry()
    }

}
