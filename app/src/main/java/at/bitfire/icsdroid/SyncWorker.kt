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

        const val IGNORE_CACHE = "IgnoreCache"


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
                .setInputData(workDataOf(IGNORE_CACHE to ignoreCache))

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
        val ignoreCache = inputData.getBoolean(IGNORE_CACHE, false)
        applicationContext.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)?.let { providerClient ->
            try {
                return withContext(Dispatchers.Default) {
                    performSync(AppAccount.get(applicationContext), providerClient, ignoreCache)
                }
            } finally {
                providerClient.closeCompat()
            }
        }
        return Result.failure()
    }

    private suspend fun performSync(account: Account, provider: ContentProviderClient, ignoreCache: Boolean): Result {
        Log.i(Constants.TAG, "Synchronizing ${account.name}. Ignore cache: $ignoreCache")
        try {
            LocalCalendar.findAll(account, provider)
                .map {
                    if (ignoreCache) {
                        it.lastModified = 0
                        it.eTag = null
                    }
                    it
                }
                .filter { it.isSynced }
                .forEach { ProcessEventsTask(applicationContext, it, ignoreCache).sync() }

        } catch (e: CalendarStorageException) {
            Log.e(Constants.TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Thread interrupted", e)
        }

        return Result.success()
    }

}
