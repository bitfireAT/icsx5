package at.bitfire.icsdroid;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.AndroidEvent;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import lombok.Cleanup;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private final static String TAG = "ICSdroid.SyncAdapter";

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
                    syncCalendar(calendar);

        } catch (CalendarStorageException e) {
            Log.e(TAG, "Calendar storage exception", e);
        }

    }

    void syncCalendar(LocalCalendar calendar) throws CalendarStorageException {
        Event[] events = null;
        try {
            Log.i(TAG, "Fetching remote calendar " + calendar.url);
            URL url = new URL(calendar.url);
            URLConnection conn = url.openConnection();

            @Cleanup InputStream is = conn.getInputStream();
            events = Event.fromStream(is, null);
        } catch (IOException|InvalidCalendarException e) {
            Log.e(TAG, "Couldn't fetch/parse calendar file", e);
        }

        Log.i(TAG, "Processing " + events.length + " events");
        List<String> uids = new LinkedList<>();
        for (Event event : events) {
            String uid = event.uid;
            uids.add(uid);
            Log.d(TAG, "Found event: " + uid);

            LocalEvent[] localEvents = calendar.findByUID(uid);
            Log.d(TAG, "Found " + localEvents.length + " local events with uid=" + uid);
            if (localEvents.length == 0) {
                Log.d(TAG, uid + " not in local calendar, adding");
                new LocalEvent(calendar, event).add();

            } else if (event.sequence > 0) {
                // SEQUENCE >0, may contain an updated version
                AndroidEvent localEvent = localEvents[0];
                if (localEvent.getEvent().sequence < event.sequence) {
                    Log.d(TAG, event.uid + " has higher SEQUENCE than in local calendar, updating");
                    localEvent.update(event);
                }
            }
        }

        Log.i(TAG, "Deleting old events (retaining " + uids.size() + " events by UID) …");
        final int deleted = calendar.retainByUID(uids.toArray(new String[uids.size()]));
        Log.i(TAG, "… " + deleted + " events deleted");
    }

}
