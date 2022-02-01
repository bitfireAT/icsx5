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
import okhttp3.MediaType
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

class ProcessEventsTask(
        val context: Context,
        val calendar: LocalCalendar
): Runnable {

    override fun run() {
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

    private fun processEvents() {
        val uri: Uri
        try {
            uri = Uri.parse(calendar.url)
        } catch(e: MalformedURLException) {
            Log.e(Constants.TAG, "Invalid calendar URL", e)
            calendar.updateStatusError(e.localizedMessage ?: e.toString())
            return
        }
        Log.i(Constants.TAG, "Synchronizing $uri")

        // dismiss old notifications
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(calendar.id.toString(), 0)
        var exception: Throwable? = null

        val downloader = object: CalendarFetcher(context, uri) {
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?) {
                InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                    try {
                        val events = Event.eventsFromReader(reader)
                        processEvents(events)

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

            override fun onNewPermanentUrl(target: URL) {
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

        if (calendar.eTag != null)
            downloader.ifNoneMatch = calendar.eTag
        if (calendar.lastModified != 0L)
            downloader.ifModifiedSince = calendar.lastModified

        downloader.run()

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

    private fun processEvents(events: List<Event>) {
        Log.i(Constants.TAG, "Processing ${events.size} events")
        val uids = HashSet<String>(events.size)

        for (event in events) {
            val uid = event.uid!!
            Log.d(Constants.TAG, "Found VEVENT: $uid")
            uids += uid

            val localEvents = calendar.queryByUID(uid)
            if (localEvents.isEmpty()) {
                Log.d(Constants.TAG, "$uid not in local calendar, adding")
                LocalEvent(calendar, event).add()

            } else {
                val localEvent = localEvents.first()
                var lastModified = event.lastModified
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
