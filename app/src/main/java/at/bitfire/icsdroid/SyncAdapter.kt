/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.content.*
import android.os.Bundle
import androidx.work.WorkManager
import at.bitfire.icsdroid.ui.NotificationUtils

class SyncAdapter(
    context: Context
): AbstractThreadedSyncAdapter(context, false) {

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        val manual = extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL)
        SyncWorker.run(context, manual)
    }

    override fun onSyncCanceled(thread: Thread?) = onSyncCanceled()
    override fun onSyncCanceled() {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(SyncWorker.NAME)
    }

    /**
     * Called by the sync framework when we don't have calendar permissions.
     */
    override fun onSecurityException(account: Account?, extras: Bundle?, authority: String?, syncResult: SyncResult?) {
        NotificationUtils.showCalendarPermissionNotification(context)
    }

}