/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import androidx.work.*
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.util.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.db.LocalCalendar
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
         * Enqueues a sync job for immediate execution. If the sync is forced,
         * the "requires network connection" constraint won't be set.
         *
         * @param context     required for managing work
         * @param force       *true* enqueues the sync regardless of the network state; *false* adds a [NetworkType.CONNECTED] constraint
         * @param ignoreCache *true* ignores all locally stored data and fetched everything from the server again
         */
        fun run(context: Context, force: Boolean = false, ignoreCache: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(workDataOf(FORCE_RESYNC to ignoreCache))

            val policy: ExistingWorkPolicy
            if (force) {
                Log.i(Constants.TAG, "Manual sync, ignoring network condition")

                // overwrite existing syncs (which may have unwanted constraints)
                policy = ExistingWorkPolicy.REPLACE

            } else {
                // regular sync, requires network
                request.setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())

                // don't overwrite previous syncs (whether regular or manual)
                policy = ExistingWorkPolicy.KEEP
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
        applicationContext.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)?.let { providerClient ->
            try {
                return withContext(Dispatchers.Default) {
                    performSync(AppAccount.get(applicationContext), providerClient, forceResync)
                }
            } finally {
                providerClient.closeCompat()
            }
        }
        return Result.failure()
    }

    private suspend fun performSync(account: Account, provider: ContentProviderClient, forceResync: Boolean): Result {
        Log.i(Constants.TAG, "Synchronizing ${account.name} (forceResync=$forceResync)")
        try {
            LocalCalendar.findAll(account, provider)
                .filter { it.isSynced }
                .forEach { ProcessEventsTask(applicationContext, it, forceResync).sync() }

        } catch (e: CalendarStorageException) {
            Log.e(Constants.TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Thread interrupted", e)
        }

        return Result.success()
    }

}
