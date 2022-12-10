/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.NotificationCompat
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.Event
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.LocalEvent
import at.bitfire.icsdroid.ui.EditCalendarActivity
import at.bitfire.icsdroid.ui.NotificationUtils
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.PropertyList
import net.fortuna.ical4j.model.component.VAlarm
import net.fortuna.ical4j.model.property.Action
import net.fortuna.ical4j.model.property.Description
import net.fortuna.ical4j.model.property.Trigger
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
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
 * @param calendar     represents the subscription to be checked
 * @param forceResync  enforces that the calendar is fetched and all events are fully processed
 *                     (useful when subscription settings have been changed)
 */
class ProcessEventsTask(
    val context: Context,
    val calendar: LocalCalendar,
    val forceResync: Boolean
) {

    suspend fun sync() {
        Thread.currentThread().contextClassLoader = context.classLoader

        try {
            // provide iCalendar event color values to Android
            AndroidCalendar.insertColors(calendar.provider, calendar.account)

            processEvents()
        } catch(e: Exception) {
            Log.e(Constants.TAG, "Couldn't sync calendar", e)
            calendar.updateStatusError(e.localizedMessage ?: e.toString())
        }
        Log.i(Constants.TAG, "iCalendar file completely processed")
    }

    /**
     * Updates the alarms of the given event according to the [calendar]'s [LocalCalendar.defaultAlarmMinutes] and [LocalCalendar.ignoreEmbeddedAlerts]
     * parameters.
     * @since 20221208
     * @param event The event to update.
     * @return The given [event], with the alarms updated.
     */
    private fun updateAlarms(event: Event): Event = event.apply {
        if (calendar.ignoreEmbeddedAlerts == true) {
            // Remove all alerts
            Log.d(Constants.TAG, "Removing all alarms from ${uid}: $this")
            alarms.clear()
        }
        calendar.defaultAlarmMinutes?.let { minutes ->
            // Check if already added alarm
            val alarm = alarms.find { it.description.value.contains("*added by ICSx5") }
            if (alarm != null) return@let
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
                        val duration = Duration.ofMinutes(-minutes)
                        add(Trigger(duration))
                    }
                )
            )
        }
    }

    private suspend fun processEvents() {
        val uri =
            try {
                Uri.parse(calendar.url)
            } catch(e: MalformedURLException) {
                Log.e(Constants.TAG, "Invalid calendar URL", e)
                calendar.updateStatusError(e.localizedMessage ?: e.toString())
                return
            }
        Log.i(Constants.TAG, "Synchronizing $uri, forceResync=$forceResync")

        // dismiss old notifications
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(calendar.id.toString(), 0)
        var exception: Throwable? = null

        val downloader = object: CalendarFetcher(context, uri) {
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?, displayName: String?) {
                InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                    try {
                        val events = Event.eventsFromReader(reader)
                        processEvents(events, forceResync)

                        Log.i(Constants.TAG, "Calendar sync successful, ETag=$eTag, lastModified=$lastModified")
                        calendar.updateStatusSuccess(eTag, lastModified ?: 0L)
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "Couldn't process events", e)
                        exception = e
                    }
                }
            }

            override fun onNotModified() {
                Log.i(Constants.TAG, "Calendar has not been modified since last sync")
                calendar.updateStatusNotModified()
            }

            override fun onNewPermanentUrl(target: Uri) {
                super.onNewPermanentUrl(target)
                Log.i(Constants.TAG, "Got permanent redirect, saving new URL: $target")
                calendar.updateUrl(target.toString())
            }

            override fun onError(error: Exception) {
                Log.w(Constants.TAG, "Sync error", error)
                exception = error
            }
        }

        CalendarCredentials(context).get(calendar).let { (username, password) ->
            downloader.username = username
            downloader.password = password
        }

        if (calendar.eTag != null && !forceResync)
            downloader.ifNoneMatch = calendar.eTag
        if (calendar.lastModified != 0L && !forceResync)
            downloader.ifModifiedSince = calendar.lastModified

        downloader.fetch()

        exception?.let { ex ->
            val message = ex.localizedMessage ?: ex.message ?: ex.toString()

            val errorIntent = Intent(context, EditCalendarActivity::class.java)
            errorIntent.data = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendar.id)
            errorIntent.putExtra(EditCalendarActivity.ERROR_MESSAGE, message)
            errorIntent.putExtra(EditCalendarActivity.THROWABLE, ex)

            val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
                    .setSmallIcon(R.drawable.ic_sync_problem_white)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setGroup(context.getString(R.string.app_name))
                    .setContentTitle(context.getString(R.string.sync_error_title))
                    .setContentText(message)
                    .setSubText(calendar.displayName)
                    .setContentIntent(PendingIntent.getActivity(context, 0, errorIntent, PendingIntent.FLAG_UPDATE_CURRENT + NotificationUtils.flagImmutableCompat))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setOnlyAlertOnce(true)
            calendar.color?.let { notification.color = it }
            notificationManager.notify(calendar.id.toString(), 0, notification.build())

            calendar.updateStatusError(message)
        }
    }

    private fun processEvents(events: List<Event>, ignoreLastModified: Boolean) {
        Log.i(Constants.TAG, "Processing ${events.size} events (ignoreLastModified=$ignoreLastModified)")
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