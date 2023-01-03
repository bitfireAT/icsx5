package at.bitfire.icsdroid.db.sync

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentValues
import android.provider.CalendarContract.Calendars
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidCalendarFactory

class LocalCalendar private constructor(
    account: Account,
    provider: ContentProviderClient,
    id: Long,
): AndroidCalendar<LocalEvent>(account, provider, LocalEvent.Factory(), id) {

    companion object {
        const val COLUMN_ETAG = Calendars.CAL_SYNC1
        const val COLUMN_LAST_MODIFIED = Calendars.CAL_SYNC4
        const val COLUMN_LAST_SYNC = Calendars.CAL_SYNC5
        const val COLUMN_ERROR_MESSAGE = Calendars.CAL_SYNC6

        /**
         * Stores if the calendar's embedded alerts should be ignored.
         * @since 20221202
         */
        const val COLUMN_IGNORE_EMBEDDED = Calendars.CAL_SYNC8

        /**
         * Stores the default alarm to set to all events in the given calendar.
         * @since 20221202
         */
        const val COLUMN_DEFAULT_ALARM = Calendars.CAL_SYNC7
    }

    /** URL of iCalendar file */
    var url: String? = null
    /** iCalendar ETag at last successful sync */
    var eTag: String? = null

    /** iCalendar Last-Modified at last successful sync (or 0 for none) */
    var lastModified = 0L
    /** time of last sync (0 if none) */
    var lastSync = 0L
    /** error message (HTTP status or exception name) of last sync (or null) */
    var errorMessage: String? = null

    /** Setting: whether to ignore alarms embedded in the Webcal */
    var ignoreEmbeddedAlerts: Boolean? = null
    /** Setting: Shall a default alarm be added to every event in the calendar? If yes, this
     *  field contains the minutes before the event. If no, it is *null*. */
    var defaultAlarmMinutes: Long? = null

    override fun populate(info: ContentValues) {
        super.populate(info)
        url = info.getAsString(Calendars.NAME)

        eTag = info.getAsString(COLUMN_ETAG)
        info.getAsLong(COLUMN_LAST_MODIFIED)?.let { lastModified = it }

        info.getAsLong(COLUMN_LAST_SYNC)?.let { lastSync = it }
        errorMessage = info.getAsString(COLUMN_ERROR_MESSAGE)

        info.getAsBoolean(COLUMN_IGNORE_EMBEDDED)?.let { ignoreEmbeddedAlerts = it }
        info.getAsLong(COLUMN_DEFAULT_ALARM)?.let { defaultAlarmMinutes = it }
    }

    class Factory: AndroidCalendarFactory<LocalCalendar> {
        override fun newInstance(account: Account, provider: ContentProviderClient, id: Long): LocalCalendar =
            LocalCalendar(account, provider, id)
    }

}
