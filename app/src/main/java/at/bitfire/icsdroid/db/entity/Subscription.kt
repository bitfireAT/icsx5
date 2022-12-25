package at.bitfire.icsdroid.db.entity

import android.content.Context
import android.database.SQLException
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidEvent
import at.bitfire.icsdroid.db.AppDatabase

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
    val url: String? = null,
    val eTag: String? = null,

    val displayName: String? = null,

    val lastModified: Long = 0L,
    val lastSync: Long = 0L,
    val errorMessage: String? = null,

    val ignoreEmbeddedAlerts: Boolean? = null,
    val defaultAlarmMinutes: Long? = null,

    val color: Int? = null
) {
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
        result = 31 * result + (ignoreEmbeddedAlerts?.hashCode() ?: 0)
        result = 31 * result + (defaultAlarmMinutes?.hashCode() ?: 0)
        result = 31 * result + (color ?: 0)
        return result
    }
}
