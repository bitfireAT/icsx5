/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.os.RemoteException
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidCalendarFactory
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.util.MiscUtils.UriHelper.asSyncAdapter

class LocalCalendar private constructor(
    account: Account,
    provider: ContentProviderClient,
    id: Long
) : AndroidCalendar<LocalEvent>(account, provider, LocalEvent.Factory, id) {

    companion object {

        const val DEFAULT_COLOR = 0xFF2F80C7.toInt()

        const val COLUMN_ETAG = Calendars.CAL_SYNC1
        const val COLUMN_LAST_MODIFIED = Calendars.CAL_SYNC4
        const val COLUMN_LAST_SYNC = Calendars.CAL_SYNC5
        const val COLUMN_ERROR_MESSAGE = Calendars.CAL_SYNC6

        /**
         * Stores if the calendar's embedded alerts should be ignored.
         */
        const val COLUMN_IGNORE_EMBEDDED = Calendars.CAL_SYNC8

        /**
         * Stores the default alarm to set to all events in the given calendar.
         */
        const val COLUMN_DEFAULT_ALARM = Calendars.CAL_SYNC7

        /**
         * Gets the calendar provider for a given context.
         * The caller (you) is responsible for closing the client!
         *
         * @throws CalendarStorageException if the calendar provider is not available
         * @throws SecurityException if permissions for accessing the calendar are not granted
         */
        fun getCalendarProvider(context: Context): ContentProviderClient =
            context.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY) ?:
            throw CalendarStorageException("Calendar provider not available")


        // CRUD methods

        fun findById(account: Account, provider: ContentProviderClient, id: Long) =
            findByID(account, provider, Factory, id)

        fun findAll(account: Account, provider: ContentProviderClient) =
            find(account, provider, Factory, null, null)

    }

    /** URL of iCalendar file */
    var url: String? = null
    /** iCalendar ETag at last successful sync */
    var eTag: String? = null

    /** iCalendar Last-Modified at last successful sync (or 0 for none) */
    var lastModified = 0L
    /** time of last sync (0 if none) */
    var lastSync = 0L
    /** error message (HTTP status or exception name) of last sync (or null) */
    var errorMessage: String? = null

    /** Setting: whether to ignore alarms embedded in the Webcal */
    var ignoreEmbeddedAlerts: Boolean? = null
    /** Setting: Shall a default alarm be added to every event in the calendar? If yes, this
     *  field contains the minutes before the event. If no, it is *null*. */
    var defaultAlarmMinutes: Long? = null


    override fun populate(info: ContentValues) {
        super.populate(info)
        url = info.getAsString(Calendars.NAME)

        eTag = info.getAsString(COLUMN_ETAG)
        info.getAsLong(COLUMN_LAST_MODIFIED)?.let { lastModified = it }

        info.getAsLong(COLUMN_LAST_SYNC)?.let { lastSync = it }
        errorMessage = info.getAsString(COLUMN_ERROR_MESSAGE)

        info.getAsBoolean(COLUMN_IGNORE_EMBEDDED)?.let { ignoreEmbeddedAlerts = it }
        info.getAsLong(COLUMN_DEFAULT_ALARM)?.let { defaultAlarmMinutes = it }
    }


    fun queryByUID(uid: String) =
        queryEvents("${Events._SYNC_ID}=?", arrayOf(uid))

    fun retainByUID(uids: MutableSet<String>): Int {
        var deleted = 0
        try {
            provider.query(
                Events.CONTENT_URI.asSyncAdapter(account),
                arrayOf(Events._ID, Events._SYNC_ID, Events.ORIGINAL_SYNC_ID),
                "${Events.CALENDAR_ID}=? AND ${Events.ORIGINAL_SYNC_ID} IS NULL", arrayOf(id.toString()), null
            )?.use { row ->
                while (row.moveToNext()) {
                    val eventId = row.getLong(0)
                    val syncId = row.getString(1)
                    if (!uids.contains(syncId)) {
                        provider.delete(ContentUris.withAppendedId(Events.CONTENT_URI, eventId).asSyncAdapter(account), null, null)
                        deleted++

                        uids -= syncId
                    }
                }
            }
            return deleted
        } catch (e: RemoteException) {
            throw CalendarStorageException("Couldn't delete local events")
        }
    }


    object Factory : AndroidCalendarFactory<LocalCalendar> {

        override fun newInstance(account: Account, provider: ContentProviderClient, id: Long) =
            LocalCalendar(account, provider, id)

    }

}