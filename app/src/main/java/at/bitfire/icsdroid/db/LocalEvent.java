/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.db;

import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.provider.CalendarContract;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.property.LastModified;

import java.io.FileNotFoundException;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.CalendarStorageException;
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

        uid = event.getUid();
        lastModified = event.getLastModified().getDateTime().getTime();
    }

    @Override
    protected void populateEvent(ContentValues values) throws FileNotFoundException, CalendarStorageException {
        super.populateEvent(values);

        getEvent().setUid(uid = values.getAsString(CalendarContract.Events._SYNC_ID));

        lastModified = values.getAsLong(COLUMN_LAST_MODIFIED);
        getEvent().setLastModified(new LastModified(new DateTime(lastModified)));
    }

    @Override
    protected void buildEvent(Event recurrence, Builder builder) throws FileNotFoundException, CalendarStorageException {
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

    }

}
