package at.bitfire.icsdroid.db.entity

import android.content.Context
import android.database.SQLException
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import at.bitfire.icsdroid.db.AppDatabase

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: Long,
    /** URL of iCalendar file */
    private var _url: String? = null,
    /** iCalendar ETag at last successful sync */
    private var _eTag: String? = null,

    /** iCalendar Last-Modified at last successful sync (or 0 for none) */
    private var _lastModified: Long = 0L,
    /** time of last sync (0 if none) */
    private var _lastSync: Long = 0L,
    /** error message (HTTP status or exception name) of last sync (or null) */
    private var _errorMessage: String? = null,

    /** Setting: whether to ignore alarms embedded in the Webcal */
    private var _ignoreEmbeddedAlerts: Boolean? = null,
    /** Setting: Shall a default alarm be added to every event in the calendar? If yes, this
     *  field contains the minutes before the event. If no, it is *null*. */
    private var _defaultAlarmMinutes: Long? = null,

    private var _color: Int? = null
) {
    /** URL of iCalendar file */
    val url: String? get() = _url
    /** iCalendar ETag at last successful sync */
    val eTag: String? get() = _eTag

    /** iCalendar Last-Modified at last successful sync (or 0 for none) */
    val lastModified: Long get() = _lastModified
    /** time of last sync (0 if none) */
    val lastSync: Long get() = _lastSync
    /** error message (HTTP status or exception name) of last sync (or null) */
    val errorMessage: String? get() = _errorMessage

    /** Setting: whether to ignore alarms embedded in the Webcal */
    val ignoreEmbeddedAlerts: Boolean? get() = _ignoreEmbeddedAlerts
    /** Setting: Shall a default alarm be added to every event in the calendar? If yes, this
     *  field contains the minutes before the event. If no, it is *null*. */
    val defaultAlarmMinutes: Long? get() = _defaultAlarmMinutes

    val color: Int? @ColorInt get() = _color

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
    suspend fun updateUrl(context: Context, url: String) {
        this._url = url

        AppDatabase.getInstance(context)
            .subscriptionsDao()
            .update(this)
    }

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
}
