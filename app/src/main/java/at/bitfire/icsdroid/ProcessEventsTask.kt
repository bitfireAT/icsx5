/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

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
import at.bitfire.icsdroid.ui.NotificationUtils
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyList
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Trigger
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Duration
import java.util.LinkedList

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
 * @param forceResync  ignores lastModified timestamp and fetches everything from the server
 */
class ProcessEventsTask(
    val context: Context,
    val subscription: Subscription,
    val calendar: LocalCalendar,
    val forceResync: Boolean
) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ProcessEventsTaskEntryPoint {
        fun appDatabase(): AppDatabase
    }

    val db = EntryPointAccessors.fromApplication(context, ProcessEventsTaskEntryPoint::class.java).appDatabase()

    private var exception: Throwable? = null

    suspend fun sync(): Boolean {
        Thread.currentThread().contextClassLoader = context.classLoader

        try {
            processEvents()
            dismissNotification()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Couldn't sync calendar", e)
            db.subscriptionsDao().updateStatusError(subscription.id, e.localizedMessage ?: e.toString())
            exception = e
            notifyError()
        }
        Log.i(Constants.TAG, "iCalendar file completely processed")

        return exception == null
    }

    /**
     * Adds an alarm to the given list of alarms, with the given [alarmMinutes] before the event.
     */
    private fun addAlarm(alarms: LinkedList<VAlarm>, alarmMinutes: Long) {
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
            Log.d(Constants.TAG, "Removing all alarms from $uid")
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
            // Add the alarm to the event
            addAlarm(alarms, alarmMinutes)
            // and also to all the exceptions
            for (exception in exceptions) {
                addAlarm(exception.alarms, alarmMinutes)
            }
        }
    }

    /**
     * Updates the advanced preferences of the given event according to the [subscription]'s:
     * - [Subscription.ignoreDescription]
     */
    private fun updateAdvancedPreferences(event: Event): Event = event.apply {
        if (subscription.ignoreDescription) {
            // Remove the description
            Log.d(Constants.TAG, "Removing the description from $uid")
            description = null
        }
    }

    private suspend fun processEvents() {
        val uri = subscription.url
        Log.i(Constants.TAG, "Synchronizing $uri, forceResync=$forceResync")

        // dismiss old notifications
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(subscription.id.toString(), 0)

        val downloader = object : CalendarFetcher(context, uri) {
            override suspend fun onSuccess(
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
                        db.subscriptionsDao().updateStatusSuccess(subscription.id, eTag, lastModified)
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "Couldn't process events", e)
                        exception = e
                    }
                }
            }

            override suspend fun onNotModified() {
                Log.i(Constants.TAG, "Calendar has not been modified since last sync")
                db.subscriptionsDao().updateStatusNotModified(subscription.id)
            }

            override suspend fun onNewPermanentUrl(target: Uri) {
                super.onNewPermanentUrl(target)
                Log.i(Constants.TAG, "Got permanent redirect, saving new URL: $target")
                db.subscriptionsDao().updateUrl(subscription.id, target)
            }

            override suspend fun onError(error: Exception) {
                Log.w(Constants.TAG, "Sync error", error)
                exception = error
            }

        }

        // Get the credentials for the given subscription from the database
        db.credentialsDao()
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

        exception?.let { e ->
            db.subscriptionsDao().updateStatusError(subscription.id, e.localizedMessage ?: e.toString())
            notifyError()
        }
    }

    private fun processEvents(events: List<Event>, ignoreLastModified: Boolean) {
        Log.i(
            Constants.TAG,
            "Processing ${events.size} events (ignoreLastModified=$ignoreLastModified)"
        )
        val uids = HashSet<String>(events.size)

        for (ev in events) {
            val event = updateAlarms(ev).let(::updateAdvancedPreferences)
            val uid = event.uid!!
            Log.d(Constants.TAG, "Found VEVENT $uid: $event")
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

    /**
     * Sends a notification to the user that the sync failed.
     * Uses the exception in [exception].
     */
    private fun notifyError() {
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
        subscription.color?.let { notification.color = it }
        notificationManager.notify(subscription.id.toInt(), notification.build())
    }

    /**
     * If [exception] is not null, dismisses the notification for the subscription.
     */
    private fun dismissNotification() {
        // If there's an error, do not dismiss the notification
        if (exception != null) return
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(subscription.id.toInt())
    }
}