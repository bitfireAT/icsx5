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
import at.bitfire.ical4android.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.db.LocalCalendar
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class SyncWorker(
        val context: Context,
        workerParams: WorkerParameters
): Worker(context, workerParams) {

    companion object {

        const val NAME = "SyncWorker"

        /**
         * Enqueues a sync job for soon execution. If the sync is forced,
         * the "requires network connection" constraint won't be set.
         *
         * @param context     required for managing work
         * @param forceSync   *true* enqueues the sync regardless of the network state; *false* adds a [NetworkType.CONNECTED] constraint
         */
        fun run(context: Context, forceSync: Boolean) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()

            if (forceSync)
                Log.i(Constants.TAG, "Manual sync, ignoring network condition")
            else
                request.setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())

            WorkManager.getInstance(context)
                    .beginUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request.build())
                    .enqueue()
        }

        fun liveStatus(context: Context) =
                WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(NAME)

    }

    private val syncQueue = LinkedBlockingQueue<Runnable>()
    private val syncExecutor = ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            5, TimeUnit.SECONDS,
            syncQueue
    )

    @SuppressLint("Recycle")
    override fun doWork(): Result {
        applicationContext.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)?.let { providerClient ->
            try {
                return performSync(AppAccount.get(context), providerClient)
            } finally {
                providerClient.closeCompat()
            }
        }
        return Result.failure()
    }

    private fun performSync(account: Account, provider: ContentProviderClient): Result {
        Log.i(Constants.TAG, "Synchronizing ${account.name}")
        try {
            LocalCalendar.findAll(account, provider)
                    .filter { it.isSynced }
                    .forEach { syncExecutor.execute(ProcessEventsTask(applicationContext, it)) }

            syncExecutor.shutdown()
            while (!syncExecutor.awaitTermination(1, TimeUnit.MINUTES))
                Log.i(Constants.TAG, "Sync still running for another minute")

        } catch (e: CalendarStorageException) {
            Log.e(Constants.TAG, "Calendar storage exception", e)
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Thread interrupted", e)
        }

        return Result.success()
    }

}
