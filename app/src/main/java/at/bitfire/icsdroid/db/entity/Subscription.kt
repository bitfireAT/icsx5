/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db.entity

import android.content.Context
import android.database.SQLException
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.LocalCalendar
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

    val lastModified: Long? = null,
    val lastSync: Long? = null,
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

    /**
     * Updates the status of a subscription that has not been modified. This is updating its [Subscription.lastSync] to the current time.
     * @param context The context that is making the request.
     * @param lastSync The synchronization time to set. Can be left as default, and will match the current system time.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    fun updateStatusNotModified(
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
    fun updateStatusSuccess(
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
    fun updateStatusError(context: Context, message: String?) =
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
    fun updateUrl(context: Context, url: Uri) =
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
    fun updateUrl(context: Context, url: String) = updateUrl(context, Uri.parse(url))

}
