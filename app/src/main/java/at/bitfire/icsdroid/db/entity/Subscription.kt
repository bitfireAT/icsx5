/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db.entity

import android.net.Uri
import android.provider.CalendarContract.Calendars
import androidx.annotation.ColorInt
import androidx.core.content.contentValuesOf
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.icsdroid.db.LocalCalendar

/**
 * Represents the storage of a subscription the user has made.
 */
@Entity(tableName = "subscriptions")
data class Subscription(
    /** The id of the subscription in the database. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** URL of iCalendar file */
    val url: Uri,
    /** ETag at last successful sync */
    val eTag: String? = null,

    /** display name of the subscription */
    val displayName: String,

    /** when the remote resource was last modified, according to its source (timestamp) */
    val lastModified: Long? = null,
    /** timestamp of last sync */
    val lastSync: Long? = null,
    /** error message (HTTP status or exception name) of last sync (or null for _no error_) */
    val errorMessage: String? = null,

    /** setting: whether to ignore alarms embedded in the Webcal */
    val ignoreEmbeddedAlerts: Boolean = false,
    /** setting: Shall a default alarm be added to every event in the calendar? If yes, this field contains the minutes before the event. If no, it is `null`. */
    val defaultAlarmMinutes: Long? = null,

    /** The color that represents the subscription. */
    val color: Int? = null
) {
    companion object {
        /**
         * The default color to use in all subscriptions.
         */
        @ColorInt
        const val DEFAULT_COLOR = 0xFF2F80C7.toInt()

        /**
         * Converts a [LocalCalendar] to a [Subscription] data object.
         * Must only be used for migrating legacy calendars.
         *
         * @param calendar The legacy calendar to create the subscription from.
         * @return A new [Subscription] that has the contents of [calendar].
         */
        fun fromLegacyCalendar(calendar: LocalCalendar) =
            Subscription(
                id = calendar.id,
                url = Uri.parse(calendar.url ?: "https://invalid-url"),
                eTag = calendar.eTag,
                displayName = calendar.displayName ?: calendar.id.toString(),
                lastModified = calendar.lastModified,
                lastSync = calendar.lastSync,
                errorMessage = calendar.errorMessage,
                ignoreEmbeddedAlerts = calendar.ignoreEmbeddedAlerts ?: false,
                defaultAlarmMinutes = calendar.defaultAlarmMinutes,
                color = calendar.color
            )

    }

    /**
     * Converts this subscription's properties to [android.content.ContentValues] that can be
     * passed to the calendar provider in order to create/update the local calendar.
     */
    fun toCalendarProperties() = contentValuesOf(
        Calendars._ID to id,
        Calendars.CALENDAR_DISPLAY_NAME to displayName,
        Calendars.CALENDAR_COLOR to color,
        Calendars.CALENDAR_ACCESS_LEVEL to Calendars.CAL_ACCESS_READ,
        Calendars.SYNC_EVENTS to 1
    )

}