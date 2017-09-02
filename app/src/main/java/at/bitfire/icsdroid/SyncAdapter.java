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
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.db.LocalCalendar;
import at.bitfire.icsdroid.db.LocalEvent;
import at.bitfire.icsdroid.ui.CalendarListActivity;
import lombok.Cleanup;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final BlockingQueue<Runnable> syncQueue = new LinkedBlockingQueue<>();
    private final ExecutorService syncExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            5, TimeUnit.SECONDS,
            syncQueue
    );

    public SyncAdapter(Context context) {
        super(context, false);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(Constants.TAG, "Synchronizing " + account.name + " on authority " + authority);

        try {
            List<LocalCalendar> calendars = LocalCalendar.findAll(account, provider);
            for (LocalCalendar calendar : calendars)
                if (calendar.isSynced())
                    syncExecutor.execute(new ProcessEventsTask(calendar, syncResult));

            syncExecutor.shutdown();
            while (!syncExecutor.awaitTermination(1, TimeUnit.MINUTES))
                Log.i(Constants.TAG, "Sync still running for another minute");

        } catch (CalendarStorageException e) {
            Log.e(Constants.TAG, "Calendar storage exception", e);
            syncResult.databaseError = true;
        } catch (InterruptedException e) {
            Log.e(Constants.TAG, "Thread interrupted", e);
        }
    }

    protected class ProcessEventsTask implements Runnable {

        final LocalCalendar calendar;
        final SyncResult syncResult;

        protected ProcessEventsTask(@NonNull LocalCalendar calendar, @NonNull SyncResult syncResult) {
            this.calendar = calendar;
            this.syncResult = syncResult;

            Thread.currentThread().setContextClassLoader(getContext().getClassLoader());
        }

        @Override
        public void run() {
            try {
                processEvents();
            } catch(CalendarStorageException e) {
                Log.e(Constants.TAG, "Couldn't access local calendars", e);
                syncResult.databaseError = true;
            }
            Log.i(Constants.TAG, "iCalendar file completely processed");
        }

        private void processEvents() throws CalendarStorageException {
            String errorMessage = null;
            URLConnection conn = null;

            URL url = null;
            try {
                url = new URL(calendar.getUrl());
            } catch (MalformedURLException e) {
                Log.e(Constants.TAG, "Invalid calendar URL", e);
                errorMessage = e.getLocalizedMessage();
            }

            boolean followRedirect = url != null;
            for (int redirect = 0; followRedirect && redirect < Constants.MAX_REDIRECTS; redirect++) {
                followRedirect = false;

                try {
                    Log.i(Constants.TAG, "Fetching remote calendar " + url);
                    conn = url.openConnection();

                    if (calendar.getLastModified() != 0)
                        conn.setIfModifiedSince(calendar.getLastModified());

                    if (conn instanceof HttpsURLConnection)
                        MTMLoader.prepareHttpsURLConnection(getContext(), (HttpsURLConnection)conn);

                    if (conn instanceof HttpURLConnection) {
                        HttpURLConnection httpConn = (HttpURLConnection)conn;
                        conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
                        conn.setRequestProperty("Connection", "close");  // workaround for AndroidHttpClient bug, which causes "Unexpected Status Line" exceptions
                        httpConn.setInstanceFollowRedirects(false);

                        if (calendar.getUsername() != null && calendar.getPassword() != null) {
                            String basicCredentials = calendar.getUsername() + ":" + calendar.getPassword();
                            conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.getBytes(), 0));
                        }

                        if (calendar.getETag() != null)
                            conn.setRequestProperty("If-None-Match", calendar.getETag());

                        final int statusCode = httpConn.getResponseCode();

                        // handle 304 Not Modified
                        if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                            conn = null;      // don't read input stream
                            calendar.updateStatusNotModified();
                            Log.i(Constants.TAG, "Calendar has not been modified since last sync (" + httpConn.getResponseMessage() + ")");

                        } else {
                            // handle redirects
                            String location = httpConn.getHeaderField("Location");
                            if (statusCode / 100 == 3 && location != null) {
                                conn = null;      // don't read input stream
                                Log.i(Constants.TAG, "Following redirect to " + location);
                                url = new URL(url, location);
                                followRedirect = true;
                                if (statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                                    Log.i(Constants.TAG, "Permanent redirect: saving new location");
                                    calendar.updateUrl(url.toString());
                                }
                            }
                        }

                        // only read stream if status is 200 OK
                        if (conn != null && statusCode != HttpURLConnection.HTTP_OK) {
                            conn = null;
                            errorMessage = statusCode + " " + httpConn.getResponseMessage();
                        }
                    } else
                        // local file, always simulate HTTP status 200 OK
                        assert conn != null;

                } catch (IOException e) {
                    Log.e(Constants.TAG, "Couldn't fetch calendar", e);
                    errorMessage = e.getLocalizedMessage();
                    synchronized(syncResult) {
                        syncResult.stats.numIoExceptions++;
                    }
                }
            }

            try {
                if (conn != null) {
                    @Cleanup InputStreamReader reader = new InputStreamReader(conn.getInputStream(),
                            MiscUtils.charsetFromContentType(conn.getContentType()));
                    List<Event> events = Event.fromReader(reader);
                    processEvents(events);

                    String eTag = conn.getHeaderField("ETag");
                    Log.i(Constants.TAG, "Calendar sync successful, saving sync state ETag=" + eTag + ", lastModified=" + conn.getLastModified());
                    calendar.updateStatusSuccess(eTag, conn.getLastModified());
                }

            } catch (IOException e) {
                Log.e(Constants.TAG, "Couldn't read calendar", e);
                errorMessage = e.getLocalizedMessage();
                synchronized(syncResult) {
                    syncResult.stats.numIoExceptions++;
                }
            } catch (InvalidCalendarException e) {
                Log.e(Constants.TAG, "Couldn't parse calendar", e);
                errorMessage = e.getLocalizedMessage();
                synchronized(syncResult) {
                    syncResult.stats.numParseExceptions++;
                }
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

        private void processEvents(List<Event> events) throws CalendarStorageException {
            Log.i(Constants.TAG, "Processing " + events.size() + " events");
            String[] uids = new String[events.size()];

            int idx = 0;
            for (Event event : events) {
                final String uid = event.getUid();
                Log.d(Constants.TAG, "Found VEVENT: " + uid);
                uids[idx++] = uid;

                List<LocalEvent> localEvents = calendar.queryByUID(uid);
                if (localEvents.size() == 0) {
                    Log.d(Constants.TAG, uid + " not in local calendar, adding");
                    new LocalEvent(calendar, event).add();
                    synchronized(syncResult) {
                        syncResult.stats.numInserts++;
                    }

                } else {
                    LocalEvent localEvent = localEvents.get(0);
                    if (event.getLastModified() == null || event.getLastModified().getDateTime().getTime() > localEvent.getLastModified()) {
                        // no LAST-MODIFIED or LAST-MODIFIED has been increased
                        localEvent.update(event);
                        synchronized(syncResult) {
                            syncResult.stats.numUpdates++;
                        }
                    } else {
                        Log.d(Constants.TAG, uid + " has not been modified since last sync");
                        synchronized(syncResult) {
                            syncResult.stats.numSkippedEntries++;
                        }
                    }
                }
            }

            Log.i(Constants.TAG, "Deleting old events (retaining " + uids.length + " events by UID) …");
            synchronized(syncResult) {
                syncResult.stats.numDeletes += calendar.retainByUID(uids);
            }
            Log.i(Constants.TAG, "… " + syncResult.stats.numDeletes + " events deleted");
        }
    }


}
