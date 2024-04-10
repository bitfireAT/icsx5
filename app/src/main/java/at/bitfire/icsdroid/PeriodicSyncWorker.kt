/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import android.util.Log
import androidx.work.*
import java.time.Duration

class PeriodicSyncWorker(
    context: Context,
    workerParams: WorkerParameters
): Worker(context, workerParams) {

    companion object {
        private const val NAME = "PeriodicSync"

        fun setInterval(context: Context, seconds: Long?) {
            val wm = WorkManager.getInstance(context)

            if (seconds != null) {
                val request = PeriodicWorkRequestBuilder<PeriodicSyncWorker>(Duration.ofSeconds(seconds))
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)      // network connection is usually required for synchronization
                        .build())
                    .build()
                wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
            } else
                wm.cancelUniqueWork(NAME)
        }

    }

    override fun doWork(): Result {
        Log.i(Constants.TAG, "Periodic worker called, running sync worker")
        SyncWorker.run(applicationContext)
        return Result.success()
    }

}