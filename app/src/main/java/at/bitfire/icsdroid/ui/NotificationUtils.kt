/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import at.bitfire.icsdroid.R

object NotificationUtils {

    const val CHANNEL_SYNC = "sync"

    const val NOTIFY_PERMISSION = 0


    val flagImmutableCompat: Int =
        if (Build.VERSION.SDK_INT >= 23)
            PendingIntent.FLAG_IMMUTABLE
        else
            0


    fun createChannels(context: Context): NotificationManager {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL_SYNC,
                    context.getString(R.string.notification_channel_sync_problem), NotificationManager.IMPORTANCE_LOW))
        }

        return nm
    }

}