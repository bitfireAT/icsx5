package at.bitfire.icsdroid.db.entity

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Calendars
import androidx.annotation.WorkerThread
import androidx.core.content.contentValuesOf
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import at.bitfire.ical4android.*
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.LocalEvent
import at.bitfire.icsdroid.db.sync.SubscriptionAndroidCalendar
import at.bitfire.icsdroid.db.sync.SubscriptionAndroidEvent
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.LastModified
import java.io.FileNotFoundException

/**
 * Represents the storage of a subscription the user has made.
 * @since 20221225
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
    @PrimaryKey val id: Long,
    val url: String,
    val eTag: String? = null,

    val displayName: String,

    val accountName: String,
    val accountType: String,

    val lastModified: Long = 0L,
    val lastSync: Long = 0L,
    val errorMessage: String? = null,

    val ignoreEmbeddedAlerts: Boolean = false,
    val defaultAlarmMinutes: Long? = null,

    val color: Int? = null
) {
    companion object {
        /**
         * Gets the calendar provider for a given context.
         * @author Arnau Mora
         * @since 20221227
         * @param context The context that is making the request.
         * @return The [ContentProviderClient] that provides an interface with the system's calendar.
         */
        fun getProvider(context: Context) = context.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
    }

    @Ignore
    val account = Account(accountName, accountType)

    // TODO: Update accordingly
    var isSynced = true
    var isVisible = true

    /**
     * Gets a [LiveData] that gets updated with the error message of the given subscription.
     * @author Arnau Mora
     * @since 20221224
     * @param context The context that is making the request.
     * @throws SQLException If any error occurs with the request.
     */
    @Throws(SQLException::class)
    fun getErrorMessageLive(context: Context): LiveData<String?> =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .getErrorMessageLive(id)

    /**
     * Updates the status of a subscription that has not been modified. This is updating its [Subscription.lastSync] to the current time.
     * @author Arnau Mora
     * @since 20221224
     * @param context The context that is making the request.
     * @param lastSync The synchronization time to set. Can be left as default, and will match the current system time.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun updateStatusNotModified(context: Context, lastSync: Long = System.currentTimeMillis()) =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .updateStatusNotModified(id, lastSync)

    /**
     * Updates the status of a subscription that has just been modified. This removes its [Subscription.errorMessage], and updates the [Subscription.eTag],
     * [Subscription.lastModified] and [Subscription.lastSync].
     * @author Arnau Mora
     * @since 20221224
     * @param context The context that is making the request.
     * @param eTag The new eTag to set.
     * @param lastModified The new date to set for [Subscription.lastModified].
     * @param lastSync The last synchronization date to set. Defaults to the current system time, so can be skipped.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun updateStatusSuccess(
        context: Context,
        eTag: String? = this.eTag,
        lastModified: Long = this.lastModified,
        lastSync: Long = System.currentTimeMillis()
    ) = AppDatabase.getInstance(context)
        .subscriptionsDao()
        .updateStatusSuccess(id, eTag, lastModified, lastSync)

    /**
     * Updates the error message of the subscription.
     * @author Arnau Mora
     * @since 20221224
     * @param context The context that is making the request.
     * @param message The error message to give to the subscription.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun updateStatusError(context: Context, message: String?) =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .updateStatusError(id, message)

    /**
     * Updates the [Subscription.url] field to the given one.
     * @author Arnau Mora
     * @since 20221224
     * @param context The context that is making the request.
     * @param url The new url to set.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun updateUrl(context: Context, url: String) =
        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .update(
                copy(url = url)
            )

    /**
     * Updates the given event's [SubscriptionEvent.id], given its [SubscriptionEvent.uid].
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @param uid The uid of the event to update.
     * @param id The new id to set to the event.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun updateEventId(context: Context, uid: String, id: Long?) =
        AppDatabase.getInstance(context)
            .eventsDao()
            .updateId(this.id, uid, id)

    /**
     * Queries a [SubscriptionEvent] from its [SubscriptionEvent.uid].
     * @author Arnau Mora
     * @since 20221224
     * @param context The context that is making the request.
     * @param uid The uid of the event.
     * @return `null` if the event was not found, otherwise, the event requested is returned.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun queryEventByUid(context: Context, uid: String) =
        AppDatabase.getInstance(context)
            .eventsDao()
            .getEventByUid(id, uid)

    /**
     * Provides an [AndroidCalendar] from the current subscription.
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @return A new calendar that matches the current subscription.
     * @throws NullPointerException If a provider could not be obtained from the [context].
     * @throws FileNotFoundException If the calendar is not available in the system's database.
     */
    @Throws(NullPointerException::class, FileNotFoundException::class)
    fun getCalendar(context: Context) = AndroidCalendar.findByID(account, getProvider(context)!!, SubscriptionAndroidCalendar.Factory(), id)

    /**
     * Removes all the events from the subscription that are not in the [uids] list.
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @param uids The list of uids to retain.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun retainByUid(context: Context, uids: Set<String>) = AppDatabase.getInstance(context)
        .eventsDao()
        .retainByUidFromSubscription(id, uids)

    /**
     * Provides iCalendar event color values to Android.
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @throws IllegalArgumentException If a provider could not be obtained from the [context].
     * @see AndroidCalendar.insertColors
     */
    fun insertColors(context: Context) =
        (getProvider(context) ?: throw IllegalArgumentException("A content provider client could not be obtained from the given context."))
            .let { provider ->
                AndroidCalendar.insertColors(provider, account)
            }

    fun queryAndroidEventById(context: Context, uid: String) = getCalendar(context).queryEvents("${Events._SYNC_ID}=?", arrayOf(uid))

    fun add(context: Context) = AndroidCalendar.create(
        account,
        getProvider(context)!!,
        contentValuesOf(
            Calendars._ID to id,
            Calendars.ACCOUNT_NAME to account.name,
            Calendars.ACCOUNT_TYPE to account.type,
            Calendars.NAME to url,
            Calendars.CALENDAR_DISPLAY_NAME to displayName,
            Calendars.CALENDAR_COLOR to color,
            Calendars.OWNER_ACCOUNT to account.name,
            Calendars.SYNC_EVENTS to 1,
            Calendars.VISIBLE to 1,
            Calendars.CALENDAR_ACCESS_LEVEL to Calendars.CAL_ACCESS_READ,
        ),
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
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (displayName?.hashCode() ?: 0)
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
