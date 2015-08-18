package at.bitfire.icsdroid;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.provider.CalendarContract;
import android.util.Log;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidCalendarFactory;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.CalendarStorageException;

public class LocalCalendar extends AndroidCalendar {

    protected String url, eTag, lastModified;

    private LocalCalendar(Account account, ContentProviderClient providerClient, AndroidEventFactory eventFactory, long id) {
        super(account, providerClient, eventFactory, id);
    }

    public static LocalCalendar[] findAll(Account account, ContentProviderClient provider) throws CalendarStorageException {
        return (LocalCalendar[])AndroidCalendar.findAll(account, provider, Factory.FACTORY);
    }


    @Override
    protected void populate(ContentValues info) {
        super.populate(info);
        url = info.getAsString(CalendarContract.Calendars.NAME);
        eTag = info.getAsString(CalendarContract.Calendars.CAL_SYNC1);
        lastModified = info.getAsString(CalendarContract.Calendars.CAL_SYNC2);
    }

    public LocalEvent[] findByUID(String uid) throws CalendarStorageException {
        return (LocalEvent[])query(AndroidEvent.COLUMN_UID + "=?", new String[] { uid });
    }

    public int retainByUID(String[] uids) throws CalendarStorageException {
        String[] escapedUIDs = new String[uids.length];
        int idx = 0;
        for (String uid : uids)
            escapedUIDs[idx++] = DatabaseUtils.sqlEscapeString(uid);
        return delete(LocalEvent.COLUMN_UID + " NOT IN (" + StringUtils.join(escapedUIDs, ",") +")", null);
    }


    public static class Factory implements AndroidCalendarFactory {
        public static final Factory FACTORY = new Factory();

        @Override
        public LocalCalendar newInstance(Account account, ContentProviderClient provider, long id) {
            return new LocalCalendar(account, provider, LocalEvent.LocalEventFactory.FACTORY, id);
        }

        @Override
        public LocalCalendar[] newArray(int size) {
            return new LocalCalendar[size];
        }

    }

}
