/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.database.SQLException
import android.os.RemoteException
import android.provider.CalendarContract
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.content.contentValuesOf
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.util.MiscUtils.UriHelper.asSyncAdapter
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.db.entity.Subscription
import java.io.FileNotFoundException

/**
 * Provides some utility functions for interacting between [Subscription]s and [LocalCalendar]s.
 * @param context The context that will be making the movements.
 */
class DatabaseAndroidInterface(
    private val context: Context,
    private val subscription: Subscription,
) {
    companion object {
        /**
         * Gets the calendar provider for a given context.
         * @return The [ContentProviderClient] that provides an interface with the system's calendar.
         * May return null if there's no [ContentProviderClient] available for the calendar
         * authority in the system.
         * @throws SecurityException If permissions for accessing the calendar are not granted.
         */
        fun getProvider(context: Context): ContentProviderClient? =
            context.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
    }

    /**
     * Provides an [AndroidCalendar] from the current subscription.
     * @return A new calendar that matches the current subscription.
     * @throws NullPointerException If a provider could not be obtained from the [context].
     * @throws FileNotFoundException If the calendar is not available in the system's database.
     */
    fun getCalendar() = AndroidCalendar.findByID(
        AppAccount.get(context),
        getProvider(context)!!,
        LocalCalendar.Factory,
        subscription.id,
    )

    /**
     * Provides iCalendar event color values to Android.
     * @throws IllegalArgumentException If a provider could not be obtained from the [context].
     * @throws SQLException If there's any issues while updating the system's database.
     * @see AndroidCalendar.insertColors
     */
    fun insertColors() =
        (getProvider(context)
            ?: throw IllegalArgumentException("A content provider client could not be obtained from the given context."))
            .let { provider ->
                AndroidCalendar.insertColors(
                    provider,
                    AppAccount.get(context),
                )
            }

    /**
     * Removes all events from the system's calendar whose uid is not included in the [uids] list.
     * @param uids The uids to keep.
     * @return The amount of events removed.
     * @throws IllegalArgumentException If a provider could not be obtained from the [context].
     * @throws CalendarStorageException If there's an error while deleting an event.
     */
    @WorkerThread
    fun androidRetainByUid(uids: Set<String>): Int {
        Log.v(Constants.TAG, "Removing all events whose uid is not in: $uids")
        val provider = getProvider(context)
            ?: throw IllegalArgumentException("A content provider client could not be obtained from the given context.")
        var deleted = 0
        try {
            val account = AppAccount.get(context)
            provider.query(
                CalendarContract.Events.CONTENT_URI.asSyncAdapter(account),
                arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events._SYNC_ID,
                    CalendarContract.Events.ORIGINAL_SYNC_ID
                ),
                "${CalendarContract.Events.CALENDAR_ID}=? AND ${CalendarContract.Events.ORIGINAL_SYNC_ID} IS NULL",
                arrayOf(subscription.id.toString()),
                null
            )?.use { row ->
                val mutableUids = uids.toMutableSet()

                while (row.moveToNext()) {
                    val eventId = row.getLong(0)
                    val syncId = row.getString(1)
                    if (!mutableUids.contains(syncId)) {
                        Log.v(Constants.TAG, "Removing event with id $syncId.")
                        provider.delete(
                            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                                .asSyncAdapter(account), null, null
                        )
                        deleted++

                        mutableUids -= syncId
                    }
                }
            }
            return deleted
        } catch (e: RemoteException) {
            Log.e(Constants.TAG, "Could not delete local events.", e)
            throw CalendarStorageException("Couldn't delete local events")
        }
    }

    /**
     * Queries an Android Event from the System's Calendar by its uid.
     * @param uid The uid of the event.
     * @throws FileNotFoundException If the subscription still not has a Calendar in the system.
     * @throws NullPointerException If a provider could not be obtained from the [context].
     */
    fun queryAndroidEventByUid(uid: String) =
        // Fetch the calendar instance for this subscription
        getCalendar()
            // Run a query with the UID given
            .queryEvents("${CalendarContract.Events._SYNC_ID}=?", arrayOf(uid))
            // If no events are returned, just return null
            .takeIf { it.isNotEmpty() }
            // Since only one event should have the given uid, and we know the list is not
            // empty, return the first element.
            ?.first()

    /**
     * Creates a calendar in the system that matches the subscription.
     * @throws NullPointerException If the [context] given doesn't have a valid provider.
     * @throws Exception If the calendar could not be created.
     */
    @WorkerThread
    fun createAndroidCalendar() = AppAccount.get(context).let { account ->
        AndroidCalendar.create(
            account,
            getProvider(context)!!,
            contentValuesOf(
                CalendarContract.Calendars._ID to subscription.id,
                CalendarContract.Calendars.ACCOUNT_NAME to account.name,
                CalendarContract.Calendars.ACCOUNT_TYPE to account.type,
                CalendarContract.Calendars.NAME to subscription.url.toString(),
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME to subscription.displayName,
                CalendarContract.Calendars.CALENDAR_COLOR to subscription.color,
                CalendarContract.Calendars.OWNER_ACCOUNT to account.name,
                CalendarContract.Calendars.SYNC_EVENTS to if (subscription.syncEvents) 1 else 0,
                CalendarContract.Calendars.VISIBLE to if (subscription.isVisible) 1 else 0,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL to CalendarContract.Calendars.CAL_ACCESS_READ,
            ),
        )
    }

    /**
     * Deletes the Android calendar associated with this subscription.
     * @return The number of rows affected, or null if the [context] given doesn't have a valid
     * provider.
     * @throws RemoteException If there's an error while making the request.
     */
    @WorkerThread
    fun deleteAndroidCalendar() = getProvider(context)?.delete(
        CalendarContract.Calendars.CONTENT_URI.asSyncAdapter(AppAccount.get(context)),
        "${CalendarContract.Calendars._ID}=?",
        arrayOf(subscription.id.toString()),
    )
}