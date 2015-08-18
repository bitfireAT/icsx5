package at.bitfire.icsdroid;

import android.content.ContentProviderOperation.Builder;
import android.provider.CalendarContract;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.Event;

public class LocalEvent extends AndroidEvent {

    LocalEvent(AndroidCalendar calendar, long id) {
        super(calendar, id);
    }

    LocalEvent(AndroidCalendar calendar, Event event) {
        super(calendar, event);
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
