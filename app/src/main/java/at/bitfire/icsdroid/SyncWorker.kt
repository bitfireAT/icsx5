/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.Context
import android.os.Build
import android.provider.CalendarContract
import android.util.Log
import androidx.work.*
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.db.LocalCalendar
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SyncWorker(
        context: Context,
        workerParams: WorkerParameters
): Worker(context, workerParams) {

    companion object {

        private const val NAME = "SyncWorker"

        val syncRunning = AtomicBoolean()

        /**
         * Enqueues a sync job for soon execution.
         */
        fun run() {
            val request = OneTimeWorkRequestBuilder<SyncWorker>().build()
            WorkManager.getInstance()
                    .beginUniqueWork(NAME, ExistingWorkPolicy.KEEP, request)
                    .enqueue()
        }

        fun liveStatus() = WorkManager.getInstance().getWorkInfosForUniqueWorkLiveData(NAME)

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
        if (syncRunning.get()) {
            Log.w(Constants.TAG, "There's already another sync running, aborting")
            return Result.SUCCESS
        }

        applicationContext.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)?.let { providerClient ->
            try {
                syncRunning.set(true)
                return performSync(AppAccount.account, providerClient)
            } finally {
                syncRunning.set(false)
                if (Build.VERSION.SDK_INT >= 24)
                    providerClient.close()
                else
                    providerClient.release()
            }
        }
        return Result.FAILURE
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

        return Result.SUCCESS
    }

}
