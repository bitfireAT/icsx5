package at.bitfire.icsdroid

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.Event
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.LocalEvent
import at.bitfire.icsdroid.ui.CalendarListActivity
import at.bitfire.icsdroid.ui.NotificationUtils
import okhttp3.MediaType
import okhttp3.internal.http.StatusLine
import java.io.InputStream
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*

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
            calendar.updateStatusError(e.localizedMessage)
        }
        Log.i(Constants.TAG, "iCalendar file completely processed")
    }

    private fun processEvents() {
        val url: URL
        try {
            url = URL(calendar.url)
        } catch(e: MalformedURLException) {
            Log.e(Constants.TAG, "Invalid calendar URL", e)
            calendar.updateStatusError(e.localizedMessage)
            return
        }
        Log.i(Constants.TAG, "Synchronizing $url")

        // dismiss old notifications
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(calendar.id.toString(), 0)
        var errorMessage: String? = null

        val downloader = object: CalendarFetcher(context, url) {
            override fun onSuccess(data: InputStream, contentType: MediaType?, eTag: String?, lastModified: Long?) {
                InputStreamReader(data, contentType?.charset() ?: Charsets.UTF_8).use { reader ->
                    try {
                        val events = Event.eventsFromReader(reader)
                        processEvents(events)

                        Log.i(Constants.TAG, "Calendar sync successful, ETag=$eTag, lastModified=$lastModified")
                        calendar.updateStatusSuccess(eTag, lastModified ?: 0L)
                    } catch (e: Exception) {
                        Log.e(Constants.TAG, "Couldn't process events", e)
                        errorMessage = e.localizedMessage
                    }
                }
            }

            override fun onNotModified() {
                Log.i(Constants.TAG, "Calendar has not been modified since last sync")
                calendar.updateStatusNotModified()
            }

            override fun onNewPermanentUrl(newUrl: URL) {
                Log.i(Constants.TAG, "Got permanent redirect, saving new URL: $newUrl")
                calendar.updateUrl(newUrl.toString())
            }

            override fun onError(error: Exception) {
                Log.w(Constants.TAG, "Sync error", error)
                errorMessage = error.localizedMessage
            }
        }

        CalendarCredentials.getCredentials(context, calendar).let { (username, password) ->
            downloader.username = username
            downloader.password = password
        }

        if (calendar.eTag != null)
            downloader.ifNoneMatch = calendar.eTag
        if (calendar.lastModified != 0L)
            downloader.ifModifiedSince = calendar.lastModified

        downloader.run()

        errorMessage?.let { msg ->
            val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
                    .setSmallIcon(R.drawable.ic_sync_problem_white)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setGroup(context.getString(R.string.app_name))
                    .setContentTitle(context.getString(R.string.sync_error_title))
                    .setContentText(msg)
                    .setSubText(calendar.displayName)
                    .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, CalendarListActivity::class.java), 0))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setOnlyAlertOnce(true)
            calendar.color?.let { notification.color = it }
            notificationManager.notify(calendar.id.toString(), 0, notification.build())

            calendar.updateStatusError(msg)
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

                if (lastModified != null) {
                    // process LAST-MODIFIED of exceptions
                    for (exception in event.exceptions) {
                        val exLastModified = exception.lastModified
                        if (exLastModified == null) {
                            lastModified = null
                            break
                        } else if (lastModified != null && exLastModified.dateTime.after(lastModified.date))
                            lastModified = exLastModified
                    }
                }

                if (lastModified == null || lastModified.dateTime.time > localEvent.lastModified)
                    // either there is no LAST-MODIFIED, or LAST-MODIFIED has been increased
                    localEvent.update(event)
                else
                    Log.d(Constants.TAG, "$uid has not been modified since last sync")
            }
        }

        Log.i(Constants.TAG, "Deleting old events (retaining ${uids.size} events by UID) …")
        val deleted = calendar.retainByUID(uids)
        Log.i(Constants.TAG, "… $deleted events deleted")
    }

}
