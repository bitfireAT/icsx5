package at.bitfire.icsdroid.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.R;
import lombok.Cleanup;

public class ValidateCalendarFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<ResourceInfo> {
    public static final String ARG_URL = "url";

    AddAccountActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (AddAccountActivity)getActivity();

        Loader<ResourceInfo> loader = getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setCancelable(false);
        progress.setMessage(getString(R.string.please_wait));
        return progress;
    }


    // loader callbacks

    @Override
    public Loader<ResourceInfo> onCreateLoader(int id, Bundle args) {
        return new ResourceInfoLoader(getActivity(), activity.url);
    }

    @Override
    public void onLoadFinished(Loader<ResourceInfo> loader, ResourceInfo info) {
        getDialog().dismiss();

        String errorMessage = null;
        if (info.exception != null)
            errorMessage = info.exception.getLocalizedMessage();
        else if (info.statusCode != 200)
            errorMessage = info.statusCode + " " + info.statusMessage;
        
        if (errorMessage == null)
            // success, proceed to CreateCalendarFragment
            activity.getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new CalendarDetailsFragment(), "create_calendar")
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        else
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoaderReset(Loader<ResourceInfo> loader) {
    }


    // loader

    static class ResourceInfoLoader extends AsyncTaskLoader<ResourceInfo> {
        ResourceInfo info;
        boolean started;

        public ResourceInfoLoader(Context context, URL url) {
            super(context);

            info = new ResourceInfo(url);
        }

        @Override
        protected void onStartLoading() {
            synchronized(this) {
                if (started == false)
                    started = true;
                    forceLoad();
                }
        }

        @Override
        public ResourceInfo loadInBackground() {
            HttpURLConnection conn;
            try {
                conn = (HttpURLConnection) info.url.openConnection();

                info.statusCode = conn.getResponseCode();
                info.statusMessage = conn.getResponseMessage();

                // String contentType = conn.getContentType();
                // TODO find charset

                @Cleanup InputStream is = conn.getInputStream();
                Event[] events = Event.fromStream(is, null);
                info.eventsFound = events.length;

            } catch (IOException|InvalidCalendarException e) {
                info.exception = e;
            }
            return info;
        }
    }
}
