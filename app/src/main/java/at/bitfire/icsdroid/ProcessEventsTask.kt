package at.bitfire.icsdroid

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Base64
import android.util.Log
import at.bitfire.cert4android.CustomCertManager
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.Event
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.LocalEvent
import at.bitfire.icsdroid.ui.CalendarListActivity
import at.bitfire.icsdroid.ui.NotificationUtils
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLConnection
import java.util.*
import javax.net.ssl.HttpsURLConnection

class ProcessEventsTask(
        val context: Context,
        val calendar: LocalCalendar
): Runnable {

    override fun run() {
        Thread.currentThread().contextClassLoader = context.classLoader

        try {
            processEvents()
        } catch(e: CalendarStorageException) {
            Log.e(Constants.TAG, "Couldn't access local calendars", e)
        }
        Log.i(Constants.TAG, "iCalendar file completely processed")
    }

    private fun processEvents() {
        var errorMessage: String? = null

        var url: URL
        try {
            url = URL(calendar.url)
        } catch(e: MalformedURLException) {
            Log.e(Constants.TAG, "Invalid calendar URL", e)
            errorMessage = e.localizedMessage
            calendar.updateStatusError(errorMessage)
            return
        }

        // dismiss old notifications
        val notificationManager = NotificationUtils.createChannels(context)
        notificationManager.cancel(calendar.id.toString(), 0)

        var conn: URLConnection? = null
        var certManager: CustomCertManager? = null

        try {
            var followRedirect = false
            var redirect = 0
            do {
                try {
                    if (url.protocol.equals("file", true) &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                        throw IOException(context.getString(R.string.sync_permission_required))

                    Log.i(Constants.TAG, "Fetching calendar $url")
                    conn = MiscUtils.prepareConnection(url)

                    if (calendar.lastModified != 0L)
                        conn.ifModifiedSince = calendar.lastModified

                    if (conn is HttpsURLConnection) {
                        certManager = CustomCertificates.certManager(context, false)
                        CustomCertificates.prepareURLConnection(certManager, conn)
                    }

                    if (conn is HttpURLConnection) {
                        conn.setRequestProperty("Connection", "close")  // workaround for AndroidHttpClient bug, which causes "Unexpected Status Line" exceptions
                        conn.instanceFollowRedirects = false

                        val (username, password) = CalendarCredentials.getCredentials(context, calendar)
                        if (username != null && password != null) {
                            Log.i(Constants.TAG, "Adding basic authorization headers")
                            val basicCredentials = "$username:$password"
                            conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.toByteArray(), Base64.NO_WRAP))
                        }

                        val eTag = calendar.eTag
                        if (eTag != null)
                            conn.setRequestProperty("If-None-Match", eTag)

                        val statusCode = conn.responseCode

                        // handle 304 Not Modified
                        if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            Log.i(Constants.TAG, "Calendar has not been modified since last sync (${conn.responseMessage})")

                            conn.disconnect()   // don't read input stream
                            conn = null

                            calendar.updateStatusNotModified()
                        } else {
                            // handle redirects
                            val location = conn.getHeaderField("Location")
                            if (statusCode/100 == 3 && location != null) {
                                conn.disconnect()   // don't read input stream
                                conn = null

                                Log.i(Constants.TAG, "Following redirect to $location")
                                url = URL(url, location)
                                followRedirect = true
                                if (statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                                    Log.i(Constants.TAG, "Permanent redirect: saving new location")
                                    calendar.updateUrl(url.toString())
                                }
                            }
                        }

                        // only read stream if status is 200 OK
                        if (conn is HttpURLConnection && statusCode != HttpURLConnection.HTTP_OK) {
                            errorMessage = "$statusCode ${conn.responseMessage}"
                            conn.disconnect()
                            conn = null
                        }
                    } else
                    // local file, always simulate HTTP status 200 OK
                        requireNotNull(conn)

                } catch(e: IOException) {
                    Log.e(Constants.TAG, "Couldn't fetch calendar", e)
                    errorMessage = e.localizedMessage
                }
                redirect++
            } while (followRedirect && redirect < Constants.MAX_REDIRECTS)

            try {
                conn?.let {
                    InputStreamReader(it.getInputStream(), MiscUtils.charsetFromContentType(it.contentType)).use { reader ->
                        val events = Event.fromReader(reader)
                        processEvents(events)

                        val eTag = it.getHeaderField("ETag")
                        Log.i(Constants.TAG, "Calendar sync successful, saving sync state ETag=" + eTag + ", lastModified=" + it.lastModified)
                        calendar.updateStatusSuccess(eTag, it.lastModified)
                    }
                }

            } catch(e: IOException) {
                Log.e(Constants.TAG, "Couldn't read calendar", e)
                errorMessage = errorMessage ?: e.localizedMessage
            } catch(e: Exception) {
                Log.e(Constants.TAG, "Couldn't process calendar", e)
                errorMessage = errorMessage ?: e.localizedMessage
            } finally {
                (conn as? HttpURLConnection)?.disconnect()
            }
        } finally {
            certManager?.close()
        }

        errorMessage?.let { msg ->
            val notification = NotificationCompat.Builder(context, NotificationUtils.CHANNEL_SYNC)
                    .setSmallIcon(R.drawable.ic_sync_problem_white)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setGroup("ICSdroid")
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