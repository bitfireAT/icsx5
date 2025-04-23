/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.calendar

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.os.RemoteException
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidCalendarFactory
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.util.MiscUtils.asSyncAdapter

class LocalCalendar private constructor(
    account: Account,
    provider: ContentProviderClient,
    id: Long
) : AndroidCalendar<LocalEvent>(account, provider, LocalEvent.Factory, id) {

    companion object {

        const val DEFAULT_COLOR = 0xFF2F80C7.toInt()

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

        fun findManaged(account: Account, provider: ContentProviderClient) =
            find(account, provider, Factory, null, null)

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
        } catch (_: RemoteException) {
            throw CalendarStorageException("Couldn't delete local events")
        }
    }

    object Factory : AndroidCalendarFactory<LocalCalendar> {

        override fun newInstance(account: Account, provider: ContentProviderClient, id: Long) =
            LocalCalendar(account, provider, id)

    }

}