/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db.entity

import android.net.Uri
import android.provider.CalendarContract.Calendars
import androidx.core.content.contentValuesOf
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.icsdroid.getStringOrNull
import org.json.JSONObject

/**
 * Represents the storage of a subscription the user has made.
 */
@Entity(tableName = "subscriptions")
data class Subscription(
    /** The id of the subscription in the database. */
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** The id of the subscription in the system's database */
    val calendarId: Long? = null,
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
    /** setting: Shall a default alarm be added to every all-day event in the calendar? If yes, this field contains the minutes before the event. If no, it is `null`. */
    val defaultAllDayAlarmMinutes: Long? = null,

    /** If true, the `DESCRIPTION` field of events will be dropped when synchronization runs. */
    @ColumnInfo(defaultValue = "0")
    val ignoreDescription: Boolean = false,

    /** The color that represents the subscription. */
    val color: Int? = null
) {
    constructor(json: JSONObject): this(
        url = json.getString("url").toUri(),
        eTag = json.getStringOrNull("eTag"),
        displayName = json.getString("displayName"),
        lastModified = json.getStringOrNull("lastModified")?.toLongOrNull(),
        lastSync = json.getStringOrNull("lastSync")?.toLongOrNull(),
        errorMessage = json.getStringOrNull("errorMessage"),
        ignoreEmbeddedAlerts = json.getStringOrNull("ignoreEmbeddedAlerts").toBoolean(),
        defaultAlarmMinutes = json.getStringOrNull("defaultAlarmMinutes")?.toLongOrNull(),
        defaultAllDayAlarmMinutes = json.getStringOrNull("defaultAllDayAlarmMinutes")?.toLongOrNull(),
        ignoreDescription = json.getStringOrNull("ignoreDescription").toBoolean(),
        color = json.getStringOrNull("color")?.toIntOrNull(),
    )

    /**
     * Converts this subscription's properties to [android.content.ContentValues] that can be
     * passed to the calendar provider in order to create/update the local calendar.
     */
    fun toCalendarProperties() = contentValuesOf(
        Calendars.NAME to url.toString(),
        Calendars.CALENDAR_DISPLAY_NAME to displayName,
        Calendars.CALENDAR_COLOR to color,
        Calendars.CALENDAR_ACCESS_LEVEL to Calendars.CAL_ACCESS_READ,
        Calendars.SYNC_EVENTS to 1
    )

    fun toJSON(): JSONObject = JSONObject().apply {
        put("url", url)
        put("eTag", eTag)
        put("displayName", displayName)
        put("lastModified", lastModified)
        put("lastSync", lastSync)
        put("errorMessage", errorMessage)
        put("ignoreEmbeddedAlerts", ignoreEmbeddedAlerts)
        put("defaultAlarmMinutes", defaultAlarmMinutes)
        put("defaultAllDayAlarmMinutes", defaultAllDayAlarmMinutes)
        put("ignoreDescription", ignoreDescription)
        put("color", color)
    }

}