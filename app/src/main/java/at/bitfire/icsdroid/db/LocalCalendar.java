package at.bitfire.icsdroid.db;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.provider.CalendarContract.Calendars;

import org.apache.commons.lang3.StringUtils;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidCalendarFactory;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.CalendarStorageException;
import lombok.Getter;

public class LocalCalendar extends AndroidCalendar {

    protected static final String
            COLUMN_ETAG = Calendars.CAL_SYNC1,
            COLUMN_LAST_MODIFIED = Calendars.CAL_SYNC2;

    @Getter protected String url, eTag;
    @Getter long lastModified;

    private LocalCalendar(Account account, ContentProviderClient providerClient, AndroidEventFactory eventFactory, long id) {
        super(account, providerClient, eventFactory, id);
    }

    public static LocalCalendar[] findAll(Account account, ContentProviderClient provider) throws CalendarStorageException {
        return (LocalCalendar[])AndroidCalendar.findAll(account, provider, Factory.FACTORY);
    }


    @Override
    protected void populate(ContentValues info) {
        super.populate(info);
        url = info.getAsString(Calendars.NAME);
        eTag = info.getAsString(COLUMN_ETAG);

        if (info.containsKey(COLUMN_LAST_MODIFIED))
            lastModified = info.getAsLong(COLUMN_LAST_MODIFIED);
    }

    public void updateCacheInfo(String eTag, long lastModified) throws CalendarStorageException {
        ContentValues values = new ContentValues();
        if ((this.eTag = eTag) != null)
            values.put(COLUMN_ETAG, eTag);
        values.put(COLUMN_LAST_MODIFIED, this.lastModified = lastModified);
        update(values);
    }

    public LocalEvent[] queryByUID(String uid) throws CalendarStorageException {
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
