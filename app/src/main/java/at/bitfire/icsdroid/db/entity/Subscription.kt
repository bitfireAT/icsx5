package at.bitfire.icsdroid.db.entity

import android.content.Context
import android.database.SQLException
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
    var url: String? = null,
    /** iCalendar ETag at last successful sync */
    var eTag: String? = null,

    /** iCalendar Last-Modified at last successful sync (or 0 for none) */
    var lastModified: Long = 0L,
    /** time of last sync (0 if none) */
    var lastSync: Long = 0L,
    /** error message (HTTP status or exception name) of last sync (or null) */
    var errorMessage: String? = null,

    /** Setting: whether to ignore alarms embedded in the Webcal */
    var ignoreEmbeddedAlerts: Boolean? = null,
    /** Setting: Shall a default alarm be added to every event in the calendar? If yes, this
     *  field contains the minutes before the event. If no, it is *null*. */
    var defaultAlarmMinutes: Long? = null,
) {
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
}
