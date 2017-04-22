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

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.LastModified;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.Event;
import lombok.Getter;

public class LocalEvent extends AndroidEvent {

    protected static final String COLUMN_LAST_MODIFIED = CalendarContract.Events.SYNC_DATA2;

    @Getter String uid;
    @Getter Long lastModified;

    LocalEvent(AndroidCalendar calendar, long id, ContentValues baseInfo) {
        super(calendar, id, baseInfo);

        uid = baseInfo.getAsString(CalendarContract.Events._SYNC_ID);
        lastModified = baseInfo.getAsLong(COLUMN_LAST_MODIFIED);
    }

    public LocalEvent(AndroidCalendar calendar, Event event) {
        super(calendar, event);

        uid = event.uid;
        lastModified = event.lastModified.getDateTime().getTime();
    }

    @Override
    protected void populateEvent(ContentValues values) {
        super.populateEvent(values);

        uid = event.uid = values.getAsString(CalendarContract.Events._SYNC_ID);

        lastModified = values.getAsLong(COLUMN_LAST_MODIFIED);
        event.lastModified = new LastModified(new DateTime(lastModified));
    }

    @Override
    protected void buildEvent(Event recurrence, Builder builder) {
        super.buildEvent(recurrence, builder);

        if (recurrence == null) {
            // master event
            builder .withValue(CalendarContract.Events._SYNC_ID, uid)
                    .withValue(COLUMN_LAST_MODIFIED, lastModified);
        } else
            // exception
            builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, uid);
    }


    public static class LocalEventFactory implements AndroidEventFactory {
        public static final LocalEventFactory FACTORY = new LocalEventFactory();

        @Override
        public AndroidEvent newInstance(AndroidCalendar calendar, long id, ContentValues baseInfo) {
            return new LocalEvent(calendar, id, baseInfo);
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
