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

package at.bitfire.icsdroid;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.db.LocalCalendar;
import at.bitfire.icsdroid.db.LocalEvent;
import at.bitfire.icsdroid.ui.CalendarListActivity;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "ICSdroid.SyncAdapter";

    private static final Pattern regexContentTypeCharset = Pattern.compile("[; ]\\s*charset=\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE);

    public SyncAdapter(Context context) {
        super(context, false);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Synchronizing " + account.name + " on authority " + authority);

        Thread.currentThread().setContextClassLoader(getContext().getClassLoader());

        try {
            LocalCalendar[] calendars = LocalCalendar.findAll(account, provider);
            for (LocalCalendar calendar : calendars)
                if (calendar.isSynced())
                    processEvents(calendar, syncResult);

        } catch (CalendarStorageException e) {
            Log.e(TAG, "Calendar storage exception", e);
            syncResult.databaseError = true;
        }
    }

    void processEvents(LocalCalendar calendar, SyncResult syncResult) throws CalendarStorageException {
        String errorMessage = null;
        URLConnection conn = null;
        try {
            Log.i(TAG, "Fetching remote calendar " + calendar.getUrl());
            conn = new URL(calendar.getUrl()).openConnection();

            conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
            if (calendar.getUsername() != null && calendar.getPassword() != null) {
                String basicCredentials = calendar.getUsername() + ":" + calendar.getPassword();
                conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.getBytes(), 0));
            }

            if (calendar.getETag() != null)
                conn.setRequestProperty("If-None-Match", calendar.getETag());
            if (calendar.getLastModified() != 0) {
                Date date = new Date(calendar.getLastModified());
                conn.setIfModifiedSince(calendar.getLastModified());
            }

            boolean readFromStream = false;

            if (conn instanceof HttpsURLConnection)
                MTMLoader.prepareHttpsURLConnection(getContext(), (HttpsURLConnection)conn);

            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpConn = (HttpURLConnection)conn;

                final int statusCode = httpConn.getResponseCode();
                switch (statusCode) {
                    case 200:
                        readFromStream = true;
                        break;
                    case 304:
                        calendar.updateStatusNotModified();
                        Log.i(TAG, "Calendar has not been modified since last sync (" + httpConn.getResponseMessage() + ")");
                        break;
                    default:
                        errorMessage = statusCode + " " + httpConn.getResponseMessage();
                }
            } else
                readFromStream = true;

            if (readFromStream) {
                Event[] events = Event.fromStream(
                        conn.getInputStream(),
                        charsetFromContentType(conn.getHeaderField("Content-Type"))
                );
                processEvents(calendar, events, syncResult);

                String eTag = conn.getHeaderField("ETag");
                Log.i(TAG, "Calendar sync successful, saving sync state ETag=" + eTag + ", lastModified=" + conn.getLastModified());
                calendar.updateStatusSuccess(eTag, conn.getLastModified());
            }

        } catch (IOException e) {
            Log.e(TAG, "Couldn't fetch calendar", e);
            errorMessage = e.getMessage();
            syncResult.stats.numIoExceptions++;
        } catch (InvalidCalendarException e) {
            Log.e(TAG, "Couldn't parse calendar", e);
            errorMessage = e.getMessage();
            syncResult.stats.numParseExceptions++;
        } finally {
            if (conn instanceof HttpURLConnection)
                ((HttpURLConnection)conn).disconnect();
        }

        if (errorMessage != null) {
            NotificationManager nm = (NotificationManager)getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            Notification notification = new NotificationCompat.Builder(getContext())
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setCategory(NotificationCompat.CATEGORY_ERROR)
                    .setColor(calendar.getColor())
                    .setGroup("ICSdroid")
                    .setContentTitle(getContext().getString(R.string.sync_error_title))
                    .setContentText(calendar.getDisplayName())
                    .setSubText(errorMessage)
                    .setContentIntent(PendingIntent.getActivity(getContext(), 0, new Intent(getContext(), CalendarListActivity.class), 0))
                    .setAutoCancel(true)
                    .setWhen(System.currentTimeMillis())
                    .setOnlyAlertOnce(true)
                    .build();
            nm.notify(0, notification);
            calendar.updateStatusError(errorMessage);
        }
    }

    private Charset charsetFromContentType(String contentType) {
        // assume UTF-8 by default [RFC 5445 3.1.4]
        Charset charset = Charsets.UTF_8;

        if (contentType != null) {
            Matcher m = regexContentTypeCharset.matcher(contentType);
            if (m.find())
                try {
                    charset = Charset.forName(m.group(1));
                    Log.v(TAG, "Using charset " + charset.displayName());
                } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                    Log.e(TAG, "Illegal or unsupported character set, assuming UTF-8", e);
                }
        }

        return charset;
    }

    private void processEvents(LocalCalendar calendar, Event[] events, SyncResult syncResult) throws FileNotFoundException, CalendarStorageException {
        Log.i(TAG, "Processing " + events.length + " events");
        String[] uids = new String[events.length];

        int idx = 0;
        for (Event event : events) {
            final String uid = event.uid;
            Log.d(TAG, "Found VEVENT: " + uid);
            uids[idx++] = uid;

            LocalEvent[] localEvents = calendar.queryByUID(uid);
            if (localEvents.length == 0) {
                Log.d(TAG, uid + " not in local calendar, adding");
                new LocalEvent(calendar, event).add();
                syncResult.stats.numInserts++;

            } else {
                LocalEvent localEvent = localEvents[0];
                if (event.lastModified == 0 || event.lastModified > localEvent.getLastModified()) {
                    // no LAST-MODIFIED or LAST-MODIFIED has been increased
                    localEvent.update(event);
                    syncResult.stats.numUpdates++;
                } else {
                    Log.d(TAG, uid + " has not been modified since last sync");
                    syncResult.stats.numSkippedEntries++;
                }
            }
        }

        Log.i(TAG, "Deleting old events (retaining " + uids.length + " events by UID) …");
        syncResult.stats.numDeletes = calendar.retainByUID(uids);
        Log.i(TAG, "… " + syncResult.stats.numDeletes + " events deleted");
    }

}
