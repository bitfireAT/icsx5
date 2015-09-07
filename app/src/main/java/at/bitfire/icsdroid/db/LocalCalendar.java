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

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.provider.CalendarContract.Calendars;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.Date;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidCalendarFactory;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.AndroidEventFactory;
import at.bitfire.ical4android.CalendarStorageException;
import lombok.Getter;

public class LocalCalendar extends AndroidCalendar {

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
    @Getter long lastModified,      // iCalendar Last-Modified at last successful sync (or 0)
            lastSync;               // time of last sync
    @Getter String errorMessage;    // error message (HTTP status or exception name) of last sync (or null)

    private LocalCalendar(Account account, ContentProviderClient providerClient, AndroidEventFactory eventFactory, long id) {
        super(account, providerClient, eventFactory, id);
    }

    public static LocalCalendar findById(Account account, ContentProviderClient provider, long id) throws FileNotFoundException, CalendarStorageException {
        return (LocalCalendar)AndroidCalendar.findByID(account, provider, Factory.FACTORY, id);
    }

    public static LocalCalendar[] findAll(Account account, ContentProviderClient provider) throws CalendarStorageException {
        return (LocalCalendar[])AndroidCalendar.findAll(account, provider, Factory.FACTORY);
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
        if ((this.eTag = eTag) != null)
            values.put(COLUMN_ETAG, eTag);
        else
            values.putNull(COLUMN_ETAG);
        values.put(COLUMN_LAST_MODIFIED, this.lastModified = lastModified);
        values.put(COLUMN_LAST_SYNC, System.currentTimeMillis());
        values.putNull(COLUMN_ERROR_MESSAGE);
        update(values);
    }

    public void updateStatusNotModified() throws CalendarStorageException {
        ContentValues values = new ContentValues(1);
        values.put(COLUMN_LAST_SYNC, new Date().getTime());
        update(values);
    }

    public void updateStatusError(String message) throws CalendarStorageException {
        ContentValues values = new ContentValues(4);
        values.putNull(COLUMN_ETAG);
        values.putNull(COLUMN_LAST_MODIFIED);
        values.put(COLUMN_LAST_SYNC, System.currentTimeMillis());
        values.put(COLUMN_ERROR_MESSAGE, message);
        update(values);
    }

    public LocalEvent[] queryByUID(String uid) throws CalendarStorageException {
        return (LocalEvent[])queryEvents(AndroidEvent.COLUMN_UID + "=?", new String[] { uid });
    }

    public int retainByUID(String[] uids) throws CalendarStorageException {
        String[] escapedUIDs = new String[uids.length];
        int idx = 0;
        for (String uid : uids)
            escapedUIDs[idx++] = DatabaseUtils.sqlEscapeString(uid);
        return deleteEvents(LocalEvent.COLUMN_UID + " NOT IN (" + StringUtils.join(escapedUIDs, ",") +")", null);
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
