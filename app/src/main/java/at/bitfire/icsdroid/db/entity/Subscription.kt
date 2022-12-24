package at.bitfire.icsdroid.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

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
)
