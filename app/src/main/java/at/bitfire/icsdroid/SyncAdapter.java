package at.bitfire.icsdroid;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.db.LocalCalendar;
import at.bitfire.icsdroid.db.LocalEvent;
import lombok.Cleanup;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "ICSdroid.SyncAdapter";

    private static final Pattern regexContentTypeCharset = Pattern.compile("[; ]\\s*charset=\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE);

    public SyncAdapter(Context context) {
        super(context, false);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Synchronizing " + account.name + " on authority " + authority);

        LocalCalendar.init(getContext());

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
        try {
            Log.i(TAG, "Fetching remote calendar " + calendar.getUrl());
            @Cleanup("disconnect") HttpURLConnection conn = (HttpURLConnection)new URL(calendar.getUrl()).openConnection();
            conn.setRequestProperty("User-Agent", "ICSdroid/" + Constants.version + " (Android)");

            if (calendar.getETag()!= null)
                conn.setRequestProperty("If-None-Match", calendar.getETag());
            if (calendar.getLastModified() != 0) {
                Date date = new Date(calendar.getLastModified());
                conn.setRequestProperty("If-Modified-Since", DateFormatUtils.SMTP_DATETIME_FORMAT.format(calendar.getLastModified()));
            }

            final int statusCode = conn.getResponseCode();
            switch (statusCode) {
                case 200:
                    Event[] events = Event.fromStream(
                            conn.getInputStream(),
                            charsetFromContentType(conn.getHeaderField("Content-Type"))
                    );
                    processEvents(calendar, events, syncResult);

                    calendar.updateCacheInfo(conn.getHeaderField("ETag"), conn.getLastModified());
                    break;
                case 304:
                    Log.i(TAG, "Calendar hasn't been updated since last sync (" + conn.getResponseMessage() + ")");
                    break;
            }
        } catch (IOException e) {
            Log.e(TAG, "Couldn't fetch calendar", e);
            syncResult.stats.numIoExceptions++;
        } catch (InvalidCalendarException e) {
            Log.e(TAG, "Couldn't parse calendar", e);
            syncResult.stats.numParseExceptions++;
        }
    }

    private Charset charsetFromContentType(String contentType) {
        // assume UTF-8 by default [RFC 5445 3.1.4]
        Charset charset = Charsets.UTF_8;

        Matcher m = regexContentTypeCharset.matcher(contentType);
        if (m.find())
            try {
                charset = Charset.forName(m.group(1));
                Log.v(TAG, "Using charset " + charset.displayName());
            } catch(IllegalCharsetNameException|UnsupportedCharsetException e) {
                Log.e(TAG, "Illegal or unsupported character set, assuming UTF-8", e);
            }

        return charset;
    }

    private void processEvents(LocalCalendar calendar, Event[] events, SyncResult syncResult) throws CalendarStorageException {
        Log.i(TAG, "Processing " + events.length + " events");
        String[] uids = new String[events.length];

        int idx = 0;
        for (Event event : events) {
            final String uid = event.uid;
            Log.d(TAG, "Found event: " + uid);
            uids[idx++] = uid;

            LocalEvent[] localEvents = calendar.queryByUID(uid);
            if (localEvents.length == 0) {
                Log.d(TAG, uid + " not in local calendar, adding");
                new LocalEvent(calendar, event).add();
                syncResult.stats.numInserts++;

            } else {
                LocalEvent localEvent = localEvents[0];
                if (event.lastModified == 0 || event.lastModified > localEvent.getEvent().lastModified) {
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
