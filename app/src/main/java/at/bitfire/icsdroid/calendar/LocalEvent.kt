/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.calendar

import android.content.ContentValues
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidEvent
import at.bitfire.ical4android.AndroidEventFactory
import at.bitfire.ical4android.Event

class LocalEvent: AndroidEvent {

    constructor(calendar: AndroidCalendar<*>, event: Event)
            : super(calendar, event, null, null, null, 0)

    private constructor(calendar: AndroidCalendar<*>, values: ContentValues)
            : super(calendar, values)


    object Factory: AndroidEventFactory<LocalEvent> {

        override fun fromProvider(calendar: AndroidCalendar<AndroidEvent>, values: ContentValues) =
                LocalEvent(calendar, values)

    }

}
