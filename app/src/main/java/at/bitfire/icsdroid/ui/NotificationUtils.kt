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

/**
 * Provides some utilities for notifications.
 */
object NotificationUtils {

    /**
     * The name of the sync channel.
     */
    const val CHANNEL_SYNC = "sync"

    const val NOTIFY_PERMISSION = 0


    /**
     * Contains the [PendingIntent.FLAG_IMMUTABLE] on SDK level 23+.
     */
    val flagImmutableCompat: Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_IMMUTABLE
        else
            0


    /**
     * Creates all the required notification channels.
     * @param context The context that is making the request.
     * @return A reference to the [NotificationManager].
     */
    fun createChannels(context: Context): NotificationManager {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SYNC,
                    context.getString(R.string.notification_channel_sync_problem),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }

        return nm
    }

}