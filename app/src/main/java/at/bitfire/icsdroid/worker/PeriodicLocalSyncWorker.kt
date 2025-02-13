/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

class PeriodicLocalSyncWorker(
    context: Context,
    workerParams: WorkerParameters
): LocalSyncWorker(context, workerParams) {

    companion object {
        private const val NAME = "PeriodicLocalSync"

        fun setInterval(context: Context, seconds: Long?) {
            val wm = WorkManager.getInstance(context)

            if (seconds != null) {
                val request = PeriodicWorkRequestBuilder<PeriodicLocalSyncWorker>(Duration.ofSeconds(seconds))
                    .build()
                wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
            } else
                wm.cancelUniqueWork(NAME)
        }
    }
}
