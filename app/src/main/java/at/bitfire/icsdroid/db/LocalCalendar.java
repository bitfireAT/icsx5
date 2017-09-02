/*
 * Copyright (c) 2013 – 2016 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 */

package at.bitfire.icsdroid.db;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.support.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.List;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidCalendarFactory;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.CalendarStorageException;
import lombok.Getter;

public class LocalCalendar extends AndroidCalendar<LocalEvent> {

    public static final String
            COLUMN_ETAG = Calendars.CAL_SYNC1,
            COLUMN_USERNAME = Calendars.CAL_SYNC2,
            COLUMN_PASSWORD = Calendars.CAL_SYNC3,
            COLUMN_LAST_MODIFIED = Calendars.CAL_SYNC4,
            COLUMN_LAST_SYNC = Calendars.CAL_SYNC5,
            COLUMN_ERROR_MESSAGE = Calendars.CAL_SYNC6;

    @Getter protected String
            url,                    // URL of iCalendar file
            eTag,                   // iCalendar ETag at last successful sync
            username,               // HTTP username (or null if no auth. required)
            password;               // HTTP password (or null if no auth. required)
    @Getter long lastModified,      // iCalendar Last-Modified at last successful sync (or 0 for none)
            lastSync;               // time of last sync
    @Getter String errorMessage;    // error message (HTTP status or exception name) of last sync (or null)


    @Override
    public String[] eventBaseInfoColumns() {
        return new String[] { CalendarContract.Events._ID, CalendarContract.Events._SYNC_ID, LocalEvent.COLUMN_LAST_MODIFIED };
    }

    private LocalCalendar(Account account, ContentProviderClient providerClient, AndroidEventFactory eventFactory, long id) {
        super(account, providerClient, eventFactory, id);
    }

    public static LocalCalendar findById(Account account, ContentProviderClient provider, long id) throws FileNotFoundException, CalendarStorageException {
        return AndroidCalendar.findByID(account, provider, Factory.FACTORY, id);
    }

    public static List<LocalCalendar> findAll(Account account, ContentProviderClient provider) throws CalendarStorageException {
        return AndroidCalendar.find(account, provider, Factory.FACTORY, null, null);
    }


    @Override
    protected void populate(ContentValues info) {
        super.populate(info);
        url = info.getAsString(Calendars.NAME);
        username = info.getAsString(COLUMN_USERNAME);
        password = info.getAsString(COLUMN_PASSWORD);

        eTag = info.getAsString(COLUMN_ETAG);
        if (info.containsKey(COLUMN_LAST_MODIFIED))
            lastModified = info.getAsLong(COLUMN_LAST_MODIFIED);

        if (info.containsKey(COLUMN_LAST_SYNC))
            lastSync = info.getAsLong(COLUMN_LAST_SYNC);
        errorMessage = info.getAsString(COLUMN_ERROR_MESSAGE);
    }

    public void updateStatusSuccess(String eTag, long lastModified) throws CalendarStorageException {
        ContentValues values = new ContentValues(4);
        values.put(COLUMN_ETAG, this.eTag = eTag);
        values.put(COLUMN_LAST_MODIFIED, this.lastModified = lastModified);
        values.put(COLUMN_LAST_SYNC, lastSync = System.currentTimeMillis());
        values.putNull(COLUMN_ERROR_MESSAGE);
        update(values);
    }

    public void updateStatusNotModified() throws CalendarStorageException {
        ContentValues values = new ContentValues(1);
        values.put(COLUMN_LAST_SYNC, lastSync = System.currentTimeMillis());
        update(values);
    }

    public void updateStatusError(String message) throws CalendarStorageException {
        ContentValues values = new ContentValues(4);
        values.putNull(COLUMN_ETAG); eTag = null;
        values.putNull(COLUMN_LAST_MODIFIED); lastModified = 0;
        values.put(COLUMN_LAST_SYNC, lastSync = System.currentTimeMillis());
        values.put(COLUMN_ERROR_MESSAGE, errorMessage = message);
        update(values);
    }

    public void updateUrl(@NonNull String url) throws CalendarStorageException {
        ContentValues values = new ContentValues(1);
        values.put(Calendars.NAME, this.url = url);
        update(values);
    }

    public List<LocalEvent> queryByUID(String uid) throws CalendarStorageException {
        return queryEvents(CalendarContract.Events._SYNC_ID + "=?", new String[] { uid });
    }

    public int retainByUID(String[] uids) throws CalendarStorageException {
        String[] escapedUIDs = new String[uids.length];
        int idx = 0;
        for (String uid : uids)
            escapedUIDs[idx++] = DatabaseUtils.sqlEscapeString(uid);
        String sqlUIDs = StringUtils.join(escapedUIDs, ",");
        try {
            return getProvider().delete(syncAdapterURI(CalendarContract.Events.CONTENT_URI),
                    CalendarContract.Events.CALENDAR_ID + "=? AND (" +
                        CalendarContract.Events._SYNC_ID + " NOT IN (" + sqlUIDs + ") OR " +
                        CalendarContract.Events.ORIGINAL_SYNC_ID + " NOT IN (" + sqlUIDs + ")" +
                    ")", new String[] { String.valueOf(getId()) });
        } catch (RemoteException e) {
            throw new CalendarStorageException("Couldn't delete local events");
        }
    }


    public static class Factory implements AndroidCalendarFactory<LocalCalendar> {
        public static final Factory FACTORY = new Factory();

        @Override
        public LocalCalendar newInstance(Account account, ContentProviderClient provider, long id) {
            return new LocalCalendar(account, provider, LocalEvent.LocalEventFactory.FACTORY, id);
        }

    }

}
