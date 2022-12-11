/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.accounts.Account
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import at.bitfire.icsdroid.ui.CalendarListActivity
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

    override fun onSecurityException(account: Account?, extras: Bundle?, authority: String?, syncResult: SyncResult?) {
        val nm = NotificationUtils.createChannels(context)
        val askPermissionsIntent = Intent(context, CalendarListActivity::class.java)
        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
                .setSmallIcon(R.drawable.ic_sync_problem_white)
                .setContentTitle(context.getString(R.string.sync_permission_required))
                .setContentText(context.getString(R.string.sync_permission_required_sync_calendar))
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setContentIntent(PendingIntent.getActivity(context, 0, askPermissionsIntent, PendingIntent.FLAG_UPDATE_CURRENT + NotificationUtils.flagImmutableCompat))
                .setAutoCancel(true)
                .setLocalOnly(true)
                .build()
        nm.notify(NotificationUtils.NOTIFY_PERMISSION, notification)
    }

}
