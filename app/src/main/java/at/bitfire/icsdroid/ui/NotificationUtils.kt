/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import at.bitfire.icsdroid.R

object NotificationUtils {

    const val CHANNEL_SYNC = "sync"

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