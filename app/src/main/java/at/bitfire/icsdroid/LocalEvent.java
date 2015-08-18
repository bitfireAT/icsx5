package at.bitfire.icsdroid;

import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.provider.CalendarContract;
import android.util.Log;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.Event;

public class LocalEvent extends AndroidEvent {

    protected static final String COLUMN_LAST_MODIFIED = CalendarContract.Events.SYNC_DATA2;

    LocalEvent(AndroidCalendar calendar, long id) {
        super(calendar, id);
    }

    LocalEvent(AndroidCalendar calendar, Event event) {
        super(calendar, event);
    }

    @Override
    protected void populateEvent(ContentValues values) {
        super.populateEvent(values);

        if (values.containsKey(COLUMN_LAST_MODIFIED))
            event.lastModified = values.getAsLong(COLUMN_LAST_MODIFIED);
    }

    @Override
    protected void buildEvent(Builder builder) {
        super.buildEvent(builder);

        builder.withValue(COLUMN_LAST_MODIFIED, event.lastModified);
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
