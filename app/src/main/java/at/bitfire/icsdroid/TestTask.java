package at.bitfire.icsdroid;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import lombok.Cleanup;

public class TestTask extends AsyncTask<Void, Void, Void> {
    private static final String TAG = "ICSdroid";

    @Override
    protected Void doInBackground(Void... params) {
        Log.i(TAG, "Quuaaaakkk");

        // TODO https://developer.android.com/reference/android/net/http/HttpResponseCache.html

        try {
            URL url = new URL("https://tiss.tuwien.ac.at/events/ical.xhtml?locale=de&token=dd91df97-bd31-4f5b-b071-b6012d5c4526");
            URLConnection conn = url.openConnection();

            @Cleanup InputStream is = conn.getInputStream();

            Event[] events = Event.fromStream(is, null);
            for (Event event : events) {
                Log.i(TAG, "Event found: " + event.uid + " " + event.summary + " " + event.dtStart.getDate() + "-" + event.dtEnd.getDate());
            }

        } catch (IOException|InvalidCalendarException e) {
            Log.e(TAG, "Error", e);
        }

        return null;
    }
}
