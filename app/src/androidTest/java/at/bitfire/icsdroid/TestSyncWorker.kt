/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TestSyncWorker @AssistedInject constructor(
    @Assisted appContext: android.content.Context,
    @Assisted workerParams: androidx.work.WorkerParameters
) : BaseSyncWorker(appContext, workerParams)