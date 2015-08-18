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
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import lombok.Cleanup;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final static String TAG = "ICSdroid.SyncAdapter";

    private final Pattern regexContentTypeCharset = Pattern.compile("[; ]\\s*charset=\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE);

    public SyncAdapter(Context context) {
        super(context, false);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Syncing!!!");

        LocalCalendar.init(getContext());

        try {
            LocalCalendar[] calendars = LocalCalendar.findAll(account, provider);
            for (LocalCalendar calendar : calendars)
                if (calendar.isSynced())
                    processEvents(calendar);

        } catch (CalendarStorageException e) {
            Log.e(TAG, "Calendar storage exception", e);
        }

    }

    void processEvents(LocalCalendar calendar) throws CalendarStorageException {
        try {
            Log.i(TAG, "Fetching remote calendar " + calendar.url);
            @Cleanup("disconnect") HttpURLConnection conn = (HttpURLConnection)new URL(calendar.url).openConnection();
            conn.setRequestProperty("User-Agent", "ICSdroid/" + Constants.version + " (Android)");

            if (calendar.eTag != null)
                conn.setRequestProperty("If-None-Match", calendar.eTag);
            if (calendar.lastModified != 0) {
                Date date = new Date(calendar.lastModified);
                conn.setRequestProperty("If-Modified-Since", DateFormatUtils.SMTP_DATETIME_FORMAT.format(calendar.lastModified));
            }

            final int statusCode = conn.getResponseCode();
            switch (statusCode) {
                case 200:
                    Event[] events = Event.fromStream(
                            conn.getInputStream(),
                            charsetFromContentType(conn.getHeaderField("Content-Type"))
                    );
                    processEvents(calendar, events);

                    calendar.updateCacheInfo(conn.getHeaderField("ETag"), conn.getLastModified());
                    break;
                case 304:
                    Log.i(TAG, "Calendar hasn't been updated since last sync (" + conn.getResponseMessage() + ")");
                    break;
            }
        } catch (IOException|InvalidCalendarException e) {
            Log.e(TAG, "Couldn't fetch/parse calendar file", e);
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

    private void processEvents(LocalCalendar calendar, Event[] events) throws CalendarStorageException {
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

            } else {
                LocalEvent localEvent = localEvents[0];
                if (event.lastModified == 0 || event.lastModified > localEvent.getEvent().lastModified) {
                    // no LAST-MODIFIED or LAST-MODIFIED has been increased
                    localEvent.update(event);
                } else
                    Log.d(TAG, uid + " has not been modified since last sync");
            }
        }

        Log.i(TAG, "Deleting old events (retaining " + uids.length + " events by UID) …");
        final int deleted = calendar.retainByUID(uids);
        Log.i(TAG, "… " + deleted + " events deleted");
    }

}
