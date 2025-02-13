/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.content.SyncResult
import android.os.Bundle
import at.bitfire.icsdroid.ui.NotificationUtils
import at.bitfire.icsdroid.worker.BaseSyncWorker

class SyncAdapter(
    context: Context
): AbstractThreadedSyncAdapter(context, false) {

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        val manual = extras.containsKey(ContentResolver.SYNC_EXTRAS_MANUAL)
        BaseSyncWorker.run(context, manual)
    }

    override fun onSyncCanceled(thread: Thread?) = onSyncCanceled()
    override fun onSyncCanceled() {
        BaseSyncWorker.cancel(context)
    }

    /**
     * Called by the sync framework when we don't have calendar permissions.
     */
    override fun onSecurityException(account: Account?, extras: Bundle?, authority: String?, syncResult: SyncResult?) {
        NotificationUtils.showCalendarPermissionNotification(context)
    }

}