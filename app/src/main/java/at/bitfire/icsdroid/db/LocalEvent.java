/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 */

package at.bitfire.icsdroid.db;

import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.provider.CalendarContract;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.Event;

public class LocalEvent extends AndroidEvent {

    protected static final String COLUMN_LAST_MODIFIED = CalendarContract.Events.SYNC_DATA2;

    LocalEvent(AndroidCalendar calendar, long id) {
        super(calendar, id);
    }

    public LocalEvent(AndroidCalendar calendar, Event event) {
        super(calendar, event);
    }

    @Override
    protected void populateEvent(ContentValues values) {
        super.populateEvent(values);

        event.uid = values.getAsString(CalendarContract.Events._SYNC_ID);

        if (values.containsKey(COLUMN_LAST_MODIFIED))
            event.lastModified = values.getAsLong(COLUMN_LAST_MODIFIED);
    }

    @Override
    protected void buildEvent(Event recurrence, Builder builder) {
        super.buildEvent(recurrence, builder);

        if (recurrence == null) {
            // master event
            builder .withValue(CalendarContract.Events._SYNC_ID, event.uid)
                    .withValue(COLUMN_LAST_MODIFIED, event.lastModified);
        } else
            // exception
            builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, event.uid);
    }


    public static class LocalEventFactory implements AndroidEventFactory {
        public static final LocalEventFactory FACTORY = new LocalEventFactory();

        @Override
        public AndroidEvent newInstance(AndroidCalendar calendar, long id) {
            return new LocalEvent(calendar, id);
        }

        @Override
        public AndroidEvent newInstance(AndroidCalendar calendar, Event event) {
            return new LocalEvent(calendar, event);
        }

        @Override
        public AndroidEvent[] newArray(int size) {
            return new LocalEvent[size];
        }
    }

}
