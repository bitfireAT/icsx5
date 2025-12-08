/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.app.PendingIntent
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.DeadObjectException
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.closeCompat
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.NotificationUtils
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

abstract class BaseSyncWorker(
    val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    @Inject
    lateinit var db: AppDatabase

    companion object {
        /**
         * An input data (Boolean) for the Worker that tells whether the synchronization should
         * ignore the lastModified timestamp and fetch everything from the server again.
         */
        const val FORCE_RESYNC = "forceResync"

    }

    private val account = AppAccount.get(applicationContext)
    lateinit var provider: ContentProviderClient

    private var forceReSync: Boolean = false

    override suspend fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 31) {
            // Read reason from previous stop
            Log.d(Constants.TAG, "Previous worker stop reason: $stopReason")

            // Observe cancellation
            currentCoroutineContext().job.invokeOnCompletion { e ->
                if (e is CancellationException) {
                    Log.e(Constants.TAG, "Worker cancelled with reason: $stopReason")
                    notifyError(context, e)
                }
            }
        }

        // Check whether we should force a complete sync
        forceReSync = inputData.getBoolean(FORCE_RESYNC, false)

        provider = try {
            LocalCalendar.getCalendarProvider(applicationContext)
        } catch (_: SecurityException) {
            NotificationUtils.showCalendarPermissionNotification(applicationContext)
            return Result.failure()
        }

        var syncFailed = false

        try {
            // update local calendars according to the subscriptions
            updateLocalCalendars()

            // provide iCalendar event color values to Android
            val account = AppAccount.get(applicationContext)
            AndroidCalendar.insertColors(provider, account)

            // sync local calendars
            for (subscription in db.subscriptionsDao().getAll()) {
                // Make sure the subscription has a matching calendar
                subscription.calendarId ?: continue
                val calendar = LocalCalendar.findById(account, provider, subscription.calendarId)
                val success = ProcessEventsTask(
                    applicationContext,
                    subscription,
                    calendar,
                    forceReSync
                ).sync()
                // If the task has failed, set the flag
                if (!success) {
                    Log.e(Constants.TAG, "Task sync not successful")
                    syncFailed = true
                }
            }
        } catch (e: DeadObjectException) {
            /* May happen when the remote process dies or (since Android 14) when IPC (for instance
            with the calendar provider) is suddenly forbidden because our sync process was demoted
            from a "service process" to a "cached process". */
            Log.e(Constants.TAG, "Received DeadObjectException, retrying.", e)
            return Result.retry()
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Thread interrupted", e)
            return Result.retry()
        } finally {
            provider.closeCompat()
        }

        return if (syncFailed)
            Result.failure()
        else
            Result.success()
    }

    /**
     * Updates the local calendars according to the available [Subscription]s. A local calendar is
     *
     * - created if there's a [Subscription] without calendar,
     * - updated (e.g. display name) if there's a [Subscription] for this calendar,
     * - deleted if there's no [Subscription] for this calendar.
     */
    private suspend fun updateLocalCalendars() {
        // subscriptions from DB
        val subscriptions = db.subscriptionsDao().getAll()

        // local calendars from provider as Map: <Calendar ID, LocalCalendar>
        val calendars =
            LocalCalendar.findManaged(account, provider).associateBy { it.id }.toMutableMap()

        // synchronize them
        for (subscription in subscriptions) {
            val calendarId = subscription.calendarId
            val calendar = calendars.remove(calendarId)
            // note that calendar might still be null even if calendarId is not null,
            // for instance when the calendar has been removed from the system

            if (calendar == null) {
                // no local calendar yet, create it
                Log.d(
                    Constants.TAG,
                    "Creating local calendar from subscription #${subscription.id}"
                )
                // create local calendar
                val uri = AndroidCalendar.create(account, provider, subscription.toCalendarProperties())
                // update calendar ID in DB
                val newCalendarId = ContentUris.parseId(uri)
                db.subscriptionsDao().updateCalendarId(subscription.id, newCalendarId)

            } else {
                // local calendar already existing, update accordingly
                Log.d(Constants.TAG, "Updating local calendar #$calendarId from subscription")
                calendar.update(subscription.toCalendarProperties())
            }
        }

        // remove remaining calendars
        for (calendar in calendars.values) {
            Log.d(Constants.TAG, "Removing local calendar #${calendar.id} without subscription")
            calendar.delete()
        }
    }


    /**
     * Sunik's Debug-Build only
     */
    private fun notifyError(context: Context, exception: Exception?) {
        val exception = exception ?: return
        val message = exception.localizedMessage ?: exception.message ?: exception.toString()
        val errorIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ERROR_MESSAGE, message)
            putExtra(MainActivity.EXTRA_THROWABLE, exception)
        }

        val notificationManager = NotificationUtils.createChannels(context)
        val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
            .setSmallIcon(R.drawable.ic_sync_problem_white)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setGroup(context.getString(R.string.app_name))
            .setContentTitle(context.getString(R.string.sync_error_title))
            .setContentText(message)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    errorIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE
                )
            )
        notificationManager.notify(9999999, notification.build())
    }

}