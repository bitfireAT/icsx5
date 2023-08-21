/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import at.bitfire.icsdroid.R

object NotificationUtils {

    const val CHANNEL_SYNC_PROBLEMS = "sync"

    const val CHANNEL_SYNC_PROGRESS = "sync-progress"

    const val NOTIFY_PERMISSION = 0


    val flagImmutableCompat: Int =
        if (Build.VERSION.SDK_INT >= 23)
            PendingIntent.FLAG_IMMUTABLE
        else
            0


    fun createChannels(context: Context): NotificationManager {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        CHANNEL_SYNC_PROBLEMS,
                        context.getString(R.string.notification_channel_sync_problem),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = context.getString(R.string.notification_channel_sync_problem_desc)
                    },
                    NotificationChannel(
                        CHANNEL_SYNC_PROGRESS,
                        context.getString(R.string.notification_channel_sync_progress),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = context.getString(R.string.notification_channel_sync_progress_desc)
                    }
                )
            )
        }

        return nm
    }

    /**
     * Shows a notification informing the user that the calendar permission is required but has not
     * been granted.
     */
    fun showCalendarPermissionNotification(context: Context) {
        val nm = createChannels(context)
        val askPermissionsIntent = Intent(context, CalendarListActivity::class.java).apply {
            putExtra(CalendarListActivity.EXTRA_REQUEST_CALENDAR_PERMISSION, true)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC_PROBLEMS)
            .setSmallIcon(R.drawable.ic_sync_problem_white)
            .setContentTitle(context.getString(R.string.sync_permission_required))
            .setContentText(context.getString(R.string.sync_permission_required_sync_calendar))
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentIntent(PendingIntent.getActivity(context, 0, askPermissionsIntent, PendingIntent.FLAG_UPDATE_CURRENT + flagImmutableCompat))
            .setAutoCancel(true)
            .setLocalOnly(true)
            .build()
        nm.notify(NOTIFY_PERMISSION, notification)
    }

}