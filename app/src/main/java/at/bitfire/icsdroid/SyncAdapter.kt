/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.accounts.Account
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import at.bitfire.icsdroid.ui.CalendarListActivity
import at.bitfire.icsdroid.ui.NotificationUtils

class SyncAdapter(
        context: Context
): AbstractThreadedSyncAdapter(context, false) {

    override fun onPerformSync(account: Account, extras: Bundle, authority: String, provider: ContentProviderClient, syncResult: SyncResult) {
        SyncWorker.run()
    }

    override fun onSecurityException(account: Account?, extras: Bundle?, authority: String?, syncResult: SyncResult?) {
        val nm = NotificationUtils.createChannels(context)
        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
                .setSmallIcon(R.drawable.ic_sync_problem_white)
                .setContentTitle(context.getString(R.string.sync_permission_required))
                .setContentText(context.getString(R.string.sync_permission_required_sync_calendar))
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, CalendarListActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))
                .setAutoCancel(true)
                .setLocalOnly(true)
                .build()
        nm.notify(0, notification)
    }

}
