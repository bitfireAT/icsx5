package at.bitfire.icsdroid

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

class LocalSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : BaseSyncWorker(context, workerParams, { it.url.scheme?.startsWith("http") == false }) {

    companion object {

        /** The name of the worker. Tags the unique work. */
        const val NAME = "LocalSyncWorker"

        /**
         * Enqueues a sync job for immediate execution. If the sync is forced,
         * the "requires network connection" constraint won't be set.
         *
         * @param context      required for managing work
         * @param forceResync  *true* ignores all locally stored data and fetched everything from the server again
         * @param onlyMigrate  *true* only runs synchronization, without fetching data.
         */
        fun run(
            context: Context,
            forceResync: Boolean = false,
            onlyMigrate: Boolean = false
        ) {
            val request = OneTimeWorkRequestBuilder<LocalSyncWorker>()
                .setInputData(
                    workDataOf(
                        FORCE_RESYNC to forceResync,
                        ONLY_MIGRATE to onlyMigrate,
                    )
                )

            WorkManager.getInstance(context)
                .beginUniqueWork(NAME, ExistingWorkPolicy.KEEP, request.build())
                .enqueue()
        }

        fun statusFlow(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(NAME)

    }

}