/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import at.bitfire.ical4android.Event
import at.bitfire.ical4android.util.DateUtils
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.calendar.LocalEvent
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.EditCalendarActivity
import at.bitfire.icsdroid.ui.NotificationUtils
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyList
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Trigger
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration

/**
 * Fetches the .ics for a given Webcal subscription and stores the events
 * in the local calendar provider.
 *
 * By default, caches will be used:
 *
 * - for fetching a calendar by HTTP (ETag/Last-Modified),
 * - for updating the local events (will only be updated when LAST-MODIFIED is newer).
 *
 * @param context      context to work in
 * @param subscription represents the subscription to be checked
 * @param forceResync  enforces that the calendar is fetched and all events are fully processed
 *                     (useful when subscription settings have been changed)
 */
class ProcessEventsTask(
    val context: Context,
    val subscription: Subscription,
    val calendar: LocalCalendar,
    val forceResync: Boolean
) {

    private val db = AppDatabase.getInstance(context)
    private val subscriptionsDao = db.subscriptionsDao()

    suspend fun sync() {
        Thread.currentThread().contextClassLoader = context.classLoader

        try {
            processEvents()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Couldn't sync calendar", e)
            subscriptionsDao.updateStatusError(subscription.id, e.localizedMessage ?: e.toString())
            // Raise the error for handling in SyncWorker
            throw e
        }
        Log.i(Constants.TAG, "iCalendar file completely processed")
    }

    /**
     * Updates the alarms of the given event according to the [subscription]'s
     * [Subscription.defaultAlarmMinutes], [Subscription.defaultAllDayAlarmMinutes] and
     * [Subscription.ignoreEmbeddedAlerts]
     * parameters.
     * @since 20221208
     * @param event The event to update.
     * @return The given [event], with the alarms updated.
     */
    private fun updateAlarms(event: Event): Event = event.apply {
        if (subscription.ignoreEmbeddedAlerts) {
            // Remove all alerts
            Log.d(Constants.TAG, "Removing all alarms from ${uid}: $this")
            alarms.clear()
        }
        val isAllDay = DateUtils.isDate(dtStart)
        val alarmMinutes = if (isAllDay)
            subscription.defaultAllDayAlarmMinutes
        else
            subscription.defaultAlarmMinutes
        if (alarmMinutes != null) {
            // Add the default alarm to the event
            Log.d(Constants.TAG, "Adding the default alarm to ${uid}.")
            alarms.add(
                // Create the new VAlarm
                VAlarm.Factory().createComponent(
                    // Set all the properties for the alarm
                    PropertyList<Property>().apply {
                        // Set action to DISPLAY
                        add(Action.DISPLAY)
                        // Add the trigger x minutes before
                        val duration = Duration.ofMinutes(-alarmMinutes)
                        add(Trigger(duration))
                    }
                )
            )
        }
    }

    private suspend fun processEvents() {
        val uri = subscription.url
        Log.i(Constants.TAG, "Synchronizing $uri, forceResync=$forceResync")

        // dismiss old notifications
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(subscription.id.toString(), 0)
        var exception: Throwable? = null

        val downloader = object : CalendarFetcher(context, uri) {
            override fun onSuccess(
                data: InputStream,
                contentType: MediaType?,
                eTag: String?,
                lastModified: Long?,
                displayName: String?
            ) {
                InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                    try {
                        val events = Event.eventsFromReader(reader)
                        processEvents(events, forceResync)

                        Log.i(Constants.TAG, "Calendar sync successful, ETag=$eTag, lastModified=$lastModified")
                        subscriptionsDao.updateStatusSuccess(subscription.id, eTag, lastModified)
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "Couldn't process events", e)
                        exception = e
                    }
                }
            }

            override fun onNotModified() {
                Log.i(Constants.TAG, "Calendar has not been modified since last sync")
                subscriptionsDao.updateStatusNotModified(subscription.id)
            }

            override fun onNewPermanentUrl(target: Uri) {
                super.onNewPermanentUrl(target)
                Log.i(Constants.TAG, "Got permanent redirect, saving new URL: $target")
                subscriptionsDao.updateUrl(subscription.id, target)
            }

            override fun onError(error: Exception) {
                Log.w(Constants.TAG, "Sync error", error)
                exception = error
            }

        }

        // Get the credentials for the given subscription from the database
        AppDatabase.getInstance(context)
            .credentialsDao()
            .getBySubscriptionId(subscription.id)
            ?.let { (_, username, password) ->
                downloader.username = username
                downloader.password = password
            }

        if (subscription.eTag != null && !forceResync)
            downloader.ifNoneMatch = subscription.eTag
        if (subscription.lastModified != 0L && !forceResync)
            downloader.ifModifiedSince = subscription.lastModified

        downloader.fetch()

        exception?.let { ex ->
            val message = ex.localizedMessage ?: ex.message ?: ex.toString()

            val errorIntent = Intent(context, EditCalendarActivity::class.java)
            errorIntent.putExtra(EditCalendarActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
            errorIntent.putExtra(EditCalendarActivity.EXTRA_ERROR_MESSAGE, message)
            errorIntent.putExtra(EditCalendarActivity.EXTRA_THROWABLE, ex)

            val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
                .setSmallIcon(R.drawable.ic_sync_problem_white)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setGroup(context.getString(R.string.app_name))
                .setContentTitle(context.getString(R.string.sync_error_title))
                .setContentText(message)
                .setSubText(subscription.displayName)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        errorIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT + NotificationUtils.flagImmutableCompat
                    )
                )
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(true)
            subscription.color?.let { notification.color = it }
            notificationManager.notify(subscription.id.toString(), 0, notification.build())

            subscriptionsDao.updateStatusError(subscription.id, message)
        }
    }

    private fun processEvents(events: List<Event>, ignoreLastModified: Boolean) {
        Log.i(
            Constants.TAG,
            "Processing ${events.size} events (ignoreLastModified=$ignoreLastModified)"
        )
        val uids = HashSet<String>(events.size)

        for (ev in events) {
            val event = updateAlarms(ev)
            val uid = event.uid!!
            Log.d(Constants.TAG, "Found VEVENT: $uid")
            uids += uid

            val localEvents = calendar.queryByUID(uid)
            if (localEvents.isEmpty()) {
                Log.d(Constants.TAG, "$uid not in local calendar, adding")
                LocalEvent(calendar, event).add()
            } else {
                val localEvent = localEvents.first()

                var lastModified = if (ignoreLastModified) null else event.lastModified
                Log.d(Constants.TAG, "$uid already in local calendar, lastModified = $lastModified")

                if (lastModified != null) {
                    // process LAST-MODIFIED of exceptions
                    for (exception in event.exceptions) {
                        val exLastModified = exception.lastModified
                        if (exLastModified == null) {
                            lastModified = null
                            break
                        } else if (lastModified != null && exLastModified.dateTime > lastModified.date)
                            lastModified = exLastModified
                    }
                }

                if (lastModified == null || lastModified.dateTime.time > localEvent.lastModified) {
                    // either there is no LAST-MODIFIED, or LAST-MODIFIED has been increased
                    Log.d(Constants.TAG, "Updating $uid in local calendar")
                    localEvent.update(event)
                } else
                    Log.d(Constants.TAG, "$uid has not been modified since last sync")
            }
        }

        Log.i(Constants.TAG, "Deleting old events (retaining ${uids.size} events by UID) …")
        val deleted = calendar.retainByUID(uids)
        Log.i(Constants.TAG, "… $deleted events deleted")
    }

}