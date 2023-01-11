package at.bitfire.icsdroid.db.entity

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.database.SQLException
import android.net.Uri
import android.os.RemoteException
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.ical4android.util.MiscUtils.UriHelper.asSyncAdapter
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.sync.LocalCalendar
import java.io.FileNotFoundException
import java.net.MalformedURLException

/**
 * Represents the storage of a subscription the user has made.
 * @param id The id of the subscription in the database.
 * @param url URL of iCalendar file
 * @param eTag iCalendar ETag at last successful sync
 * @param displayName Display name of the subscription
 * @param lastModified iCalendar Last-Modified at last successful sync (or 0 for none)
 * @param lastSync time of last sync (0 if none)
 * @param errorMessage error message (HTTP status or exception name) of last sync (or null)
 * @param ignoreEmbeddedAlerts Setting: whether to ignore alarms embedded in the Webcal
 * @param defaultAlarmMinutes Setting: Shall a default alarm be added to every event in the calendar? If yes, this field contains the minutes before the event.
 * If no, it is `null`.
 * @param color The color that represents the subscription.
 */
@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: Long = 0L,
    val url: Uri,
    val eTag: String? = null,

    val displayName: String,

    val accountName: String,
    val accountType: String,

    val lastModified: Long = 0L,
    val lastSync: Long = 0L,
    val syncEvents: Boolean = false,
    val errorMessage: String? = null,

    val ignoreEmbeddedAlerts: Boolean = false,
    val defaultAlarmMinutes: Long? = null,

    val color: Int? = null,

    val isSynced: Boolean = true,
    val isVisible: Boolean = true,
) {
    companion object {
        /**
         * The default color to use in all subscriptions.
         */
        @ColorInt
        const val DEFAULT_COLOR = 0xFF2F80C7.toInt()

        /**
         * Gets the calendar provider for a given context.
         * @param context The context that is making the request.
         * @return The [ContentProviderClient] that provides an interface with the system's calendar.
         */
        fun getProvider(context: Context) =
            context.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)

        /**
         * Creates a [Subscription] from a [LocalCalendar].
         * @param calendar The calendar to create the subscription from.
         * @return A new subscription that has the contents of [calendar].
         * @throws IllegalArgumentException If the [calendar] doesn't have a valid display name.
         */
        fun fromCalendar(calendar: LocalCalendar) =
            Subscription(
                id = calendar.id,
                url = calendar.url!!.let { Uri.parse(it) },
                eTag = calendar.eTag,
                displayName = calendar.displayName
                    ?: throw IllegalArgumentException("Every subscription requires a displayName, and the calendar given doesn't have one."),
                accountName = calendar.account.name,
                accountType = calendar.account.type,
                lastModified = calendar.lastModified,
                lastSync = calendar.lastSync,
                errorMessage = calendar.errorMessage,
                ignoreEmbeddedAlerts = calendar.ignoreEmbeddedAlerts ?: false,
                defaultAlarmMinutes = calendar.defaultAlarmMinutes,
                color = calendar.color,
                isSynced = calendar.isSynced,
                isVisible = calendar.isVisible,
            )
    }

    @Ignore
    val account = Account(accountName, accountType)

    /**
     * Removes the subscription from the database, and its matching calendar from the system.
     * @param context The context that is making the request.
     * @throws SQLException If any error occurs when updating the database.
     * @throws RemoteException If any error occurs when updating the system's database.
     */
    @WorkerThread
    suspend fun delete(context: Context) {
        // Remove the subscription from the database
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .delete(this)

        // Remove the calendar from the system
        deleteAndroidCalendar(context)
    }

    /**
     * Updates the status of a subscription that has not been modified. This is updating its [Subscription.lastSync] to the current time.
     * @param context The context that is making the request.
     * @param lastSync The synchronization time to set. Can be left as default, and will match the current system time.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    suspend fun updateStatusNotModified(
        context: Context,
        lastSync: Long = System.currentTimeMillis()
    ) =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .updateStatusNotModified(id, lastSync)

    /**
     * Updates the status of a subscription that has just been modified. This removes its [Subscription.errorMessage], and updates the [Subscription.eTag],
     * [Subscription.lastModified] and [Subscription.lastSync].
     * @param context The context that is making the request.
     * @param eTag The new eTag to set.
     * @param lastModified The new date to set for [Subscription.lastModified].
     * @param lastSync The last synchronization date to set. Defaults to the current system time, so can be skipped.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    suspend fun updateStatusSuccess(
        context: Context,
        eTag: String? = this.eTag,
        lastModified: Long? = this.lastModified,
        lastSync: Long = System.currentTimeMillis()
    ) = AppDatabase.getInstance(context)
        .subscriptionsDao()
        .updateStatusSuccess(id, eTag, lastModified ?: 0L, lastSync)

    /**
     * Updates the error message of the subscription.
     * @param context The context that is making the request.
     * @param message The error message to give to the subscription.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    suspend fun updateStatusError(context: Context, message: String?) =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .updateStatusError(id, message)

    /**
     * Updates the [Subscription.url] field to the given one.
     * @param context The context that is making the request.
     * @param url The new url to set.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    suspend fun updateUrl(context: Context, url: Uri) =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .update(
                copy(url = url)
            )

    /**
     * Updates the [Subscription.url] field to the given one.
     * @param context The context that is making the request.
     * @param url The new url to set.
     * @throws SQLException If any error occurs with the update.
     * @throws MalformedURLException If the given [url] cannot be parsed to [Uri].
     */
    @WorkerThread
    suspend fun updateUrl(context: Context, url: String) = updateUrl(context, Uri.parse(url))

    /**
     * Provides an [AndroidCalendar] from the current subscription.
     * @param context The context that is making the request.
     * @return A new calendar that matches the current subscription.
     * @throws NullPointerException If a provider could not be obtained from the [context].
     * @throws FileNotFoundException If the calendar is not available in the system's database.
     */
    fun getCalendar(context: Context) = AndroidCalendar.findByID(
        account,
        getProvider(context)!!,
        LocalCalendar.Factory(),
        id
    )

    /**
     * Removes all the events from the subscription that are not in the [uids] list.
     * @param context The context that is making the request.
     * @param uids The list of uids to retain.
     * @throws SQLException If any error occurs with the update.
     * @throws IllegalArgumentException If a provider could not be obtained from the [context].
     * @throws CalendarStorageException If there's an error while deleting an event.
     * @return The amount of events removed.
     */
    @WorkerThread
    fun retainByUid(context: Context, uids: Set<String>): Int =
        androidRetainByUid(context, uids.toMutableSet())

    /**
     * Provides iCalendar event color values to Android.
     * @param context The context that is making the request.
     * @throws IllegalArgumentException If a provider could not be obtained from the [context].
     * @throws SQLException If there's any issues while updating the system's database.
     * @see AndroidCalendar.insertColors
     */
    fun insertColors(context: Context) =
        (getProvider(context)
            ?: throw IllegalArgumentException("A content provider client could not be obtained from the given context."))
            .let { provider ->
                AndroidCalendar.insertColors(provider, account)
            }

    /**
     * Removes all events from the system's calendar whose uid is not included in the [uids] list.
     * @param context The context that is making the request.
     * @param uids The uids to keep.
     * @return The amount of events removed.
     * @throws IllegalArgumentException If a provider could not be obtained from the [context].
     * @throws CalendarStorageException If there's an error while deleting an event.
     */
    @WorkerThread
    private fun androidRetainByUid(context: Context, uids: MutableSet<String>): Int {
        Log.v(TAG, "Removing all events whose uid is not in: $uids")
        val provider = getProvider(context)
            ?: throw IllegalArgumentException("A content provider client could not be obtained from the given context.")
        var deleted = 0
        try {
            provider.query(
                Events.CONTENT_URI.asSyncAdapter(account),
                arrayOf(Events._ID, Events._SYNC_ID, Events.ORIGINAL_SYNC_ID),
                "${Events.CALENDAR_ID}=? AND ${Events.ORIGINAL_SYNC_ID} IS NULL",
                arrayOf(id.toString()),
                null
            )?.use { row ->
                while (row.moveToNext()) {
                    val eventId = row.getLong(0)
                    val syncId = row.getString(1)
                    if (!uids.contains(syncId)) {
                        Log.v(TAG, "Removing event with id $syncId.")
                        provider.delete(
                            ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
                                .asSyncAdapter(account), null, null
                        )
                        deleted++

                        uids -= syncId
                    }
                }
            }
            return deleted
        } catch (e: RemoteException) {
            Log.e(TAG, "Could not delete local events.", e)
            throw CalendarStorageException("Couldn't delete local events")
        }
    }

    /**
     * Queries an Android Event from the System's Calendar by its uid.
     * @param context The context that is making the request.
     * @param uid The uid of the event.
     * @throws FileNotFoundException If the subscription still not has a Calendar in the system.
     * @throws NullPointerException If a provider could not be obtained from the [context].
     */
    fun queryAndroidEventByUid(context: Context, uid: String) =
        // Fetch the calendar instance for this subscription
        getCalendar(context)
            // Run a query with the UID given
            .queryEvents("${Events._SYNC_ID}=?", arrayOf(uid))
            // If no events are returned, just return null
            .takeIf { it.isNotEmpty() }
            // Since only one event should have the given uid, and we know the list is not
            // empty, return the first element.
            ?.first()

    /**
     * Creates a calendar in the system that matches the subscription.
     * @param context The context that is making the request.
     * @throws NullPointerException If the [context] given doesn't have a valid provider.
     * @throws Exception If the calendar could not be created.
     */
    @WorkerThread
    fun createAndroidCalendar(context: Context) = AndroidCalendar.create(
        account,
        getProvider(context)!!,
        contentValuesOf(
            Calendars._ID to id,
            Calendars.ACCOUNT_NAME to account.name,
            Calendars.ACCOUNT_TYPE to account.type,
            Calendars.NAME to url.toString(),
            Calendars.CALENDAR_DISPLAY_NAME to displayName,
            Calendars.CALENDAR_COLOR to color,
            Calendars.OWNER_ACCOUNT to account.name,
            Calendars.SYNC_EVENTS to if (syncEvents) 1 else 0,
            Calendars.VISIBLE to if (isVisible) 1 else 0,
            Calendars.CALENDAR_ACCESS_LEVEL to Calendars.CAL_ACCESS_READ,
        ),
    )

    /**
     * Deletes the Android calendar associated with this subscription.
     * @param context The context making the request.
     * @return The number of rows affected, or null if the [context] given doesn't have a valid
     * provider.
     * @throws RemoteException If there's an error while making the request.
     */
    @WorkerThread
    fun deleteAndroidCalendar(context: Context) = getProvider(context)?.delete(
        Calendars.CONTENT_URI.asSyncAdapter(account),
        "${Calendars._ID}=?",
        arrayOf(id.toString()),
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Subscription

        if (id != other.id) return false
        if (displayName != other.displayName) return false
        if (url != other.url) return false
        if (eTag != other.eTag) return false
        if (lastModified != other.lastModified) return false
        if (lastSync != other.lastSync) return false
        if (errorMessage != other.errorMessage) return false
        if (ignoreEmbeddedAlerts != other.ignoreEmbeddedAlerts) return false
        if (defaultAlarmMinutes != other.defaultAlarmMinutes) return false
        if (color != other.color) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + (eTag?.hashCode() ?: 0)
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + lastSync.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + ignoreEmbeddedAlerts.hashCode()
        result = 31 * result + (defaultAlarmMinutes?.hashCode() ?: 0)
        result = 31 * result + (color ?: 0)
        return result
    }
}
