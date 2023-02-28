/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.calendar

import android.content.ContentValues
import android.provider.CalendarContract
import at.bitfire.ical4android.*
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.LastModified

class LocalEvent: AndroidEvent {

    companion object {
        const val COLUMN_LAST_MODIFIED = CalendarContract.Events.SYNC_DATA2
    }

    var uid: String? = null
    var lastModified = 0L

    private constructor(calendar: AndroidCalendar<AndroidEvent>, values: ContentValues): super(calendar, values) {
        uid = values.getAsString(CalendarContract.Events._SYNC_ID)
        lastModified = values.getAsLong(COLUMN_LAST_MODIFIED) ?: 0
    }

    constructor(calendar: AndroidCalendar<AndroidEvent>, event: Event): super(calendar, event) {
        uid = event.uid
        lastModified = event.lastModified?.dateTime?.time ?: 0
    }

    override fun populateEvent(row: ContentValues, groupScheduled: Boolean) {
        super.populateEvent(row, groupScheduled)

        val event = requireNotNull(event)
        event.uid = row.getAsString(CalendarContract.Events._SYNC_ID)

        row.getAsLong(COLUMN_LAST_MODIFIED).let {
            lastModified = it
            event.lastModified = LastModified(DateTime(it))
        }
    }

    override fun buildEvent(recurrence: Event?, builder: BatchOperation.CpoBuilder) {
        super.buildEvent(recurrence, builder)

        if (recurrence == null) {
            // master event
            builder .withValue(CalendarContract.Events._SYNC_ID, uid)
                    .withValue(COLUMN_LAST_MODIFIED, lastModified)
        } else
            // exception
            builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, uid)
    }


    object Factory: AndroidEventFactory<LocalEvent> {

        override fun fromProvider(calendar: AndroidCalendar<AndroidEvent>, values: ContentValues) =
                LocalEvent(calendar, values)

    }

}
