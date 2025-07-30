/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import at.bitfire.icsdroid.Constants.TAG
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
) : BaseSyncWorker(context, workerParams) {

    companion object {

        /** The name of the worker. Tags the unique work. */
        const val NAME = "SyncWorker"

        /**
         * Enqueues a sync job for immediate execution. If the sync is forced,
         * the lastModified timestamp will be ignored and everything is fetched from the server again.
         *
         * @param context      required for managing work
         * @param force        *true* enqueues the sync regardless of the network state; *false* adds a [NetworkType.CONNECTED] constraint
         * @param forceResync  *true* ignores lastModified timestamp and fetches everything from the server
         */
        fun run(
            context: Context,
            force: Boolean = false,
            forceResync: Boolean = false
        ) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
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

        fun statusFlow(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(NAME)

    }

}