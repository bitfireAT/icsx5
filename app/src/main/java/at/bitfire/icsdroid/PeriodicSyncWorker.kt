/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

class PeriodicSyncWorker(
    context: Context,
    workerParams: WorkerParameters
): BaseSyncWorker(context, workerParams) {

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
                wm.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
            } else
                wm.cancelUniqueWork(NAME)
        }
    }
}
