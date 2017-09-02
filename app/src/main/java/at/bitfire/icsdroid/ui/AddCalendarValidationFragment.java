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

package at.bitfire.icsdroid.ui;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import at.bitfire.ical4android.Event;
import at.bitfire.ical4android.InvalidCalendarException;
import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.MTMLoader;
import at.bitfire.icsdroid.MiscUtils;
import at.bitfire.icsdroid.R;
import lombok.Cleanup;

public class AddCalendarValidationFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<ResourceInfo> {
    private static final String ARG_INFO = "info";

    public static AddCalendarValidationFragment newInstance(@NonNull ResourceInfo info) {
        AddCalendarValidationFragment frag = new AddCalendarValidationFragment();
        Bundle args = new Bundle(1);
        args.putSerializable(ARG_INFO, info);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(0, getArguments(), this);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        setCancelable(false);

        ProgressDialog progress = new ProgressDialog(getActivity());
        progress.setMessage(getString(R.string.add_calendar_validating));
        return progress;
    }


    // loader callbacks

    @Override
    public Loader<ResourceInfo> onCreateLoader(int id, Bundle args) {
        return new ResourceInfoLoader(getContext(), (ResourceInfo)args.getSerializable(ARG_INFO));
    }

    @Override
    public void onLoadFinished(Loader<ResourceInfo> loader, ResourceInfo info) {
        getDialog().dismiss();

        String errorMessage = null;
        if (info.exception != null)
            errorMessage = info.exception.getMessage();
        else if (info.statusCode != HttpURLConnection.HTTP_OK)
            errorMessage = info.statusCode + " " + info.statusMessage;

        if (errorMessage == null) {
            if (StringUtils.isEmpty(info.calendarName))
                info.calendarName = info.url.getFile();

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, AddCalendarDetailsFragment.newInstance(info))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } else
            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoaderReset(Loader<ResourceInfo> loader) {
    }


    // loader

    protected static class ResourceInfoLoader extends AsyncTaskLoader<ResourceInfo> {
        ResourceInfo info;
        boolean loaded;

        public ResourceInfoLoader(Context context, ResourceInfo info) {
            super(context);
            this.info = info;
        }

        @Override
        protected void onStartLoading() {
            synchronized(this) {
                if (!loaded) {
                    forceLoad();
                    loaded = true;
                }
            }
        }

        @Override
        public ResourceInfo loadInBackground() {
            info.exception = null;

            URLConnection conn = null;

            URL url = info.url;
            boolean followRedirect = true;
            for (int redirect = 0; followRedirect && redirect < Constants.MAX_REDIRECTS; redirect++) {
                followRedirect = false;
                try {
                    conn = url.openConnection();
                    conn.setConnectTimeout(7000);
                    conn.setReadTimeout(20000);

                    if (conn instanceof HttpsURLConnection)
                        MTMLoader.prepareHttpsURLConnection(getContext(), (HttpsURLConnection)conn);

                    if (conn instanceof HttpURLConnection) {
                        HttpURLConnection httpConn = (HttpURLConnection)conn;
                        conn.setRequestProperty("User-Agent", Constants.USER_AGENT);
                        httpConn.setInstanceFollowRedirects(false);

                        if (info.authRequired) {
                            String basicCredentials = info.username + ":" + info.password;
                            conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(basicCredentials.getBytes(), 0));

                        }
                        info.statusCode = httpConn.getResponseCode();
                        info.statusMessage = httpConn.getResponseMessage();

                        // handle redirects
                        String location = httpConn.getHeaderField("Location");
                        if (info.statusCode/100 == 3 && location != null) {
                            Log.i(Constants.TAG, "Following redirect to " + location);
                            url = new URL(url, location);
                            followRedirect = true;
                            if (info.statusCode == HttpURLConnection.HTTP_MOVED_PERM) {
                                Log.i(Constants.TAG, "Permanent redirect: saving new location");
                                info.url = url;
                            }
                        }

                        // only read stream if status is 200 OK
                        if (info.statusCode != HttpURLConnection.HTTP_OK)
                            conn = null;

                    } else
                        // local file, always simulate HTTP status 200 OK
                        info.statusCode = HttpURLConnection.HTTP_OK;

                } catch (IOException e) {
                    info.exception = e;
                }
            }

            try {
                if (conn != null) {
                    @Cleanup InputStreamReader reader = new InputStreamReader(conn.getInputStream(),
                            MiscUtils.charsetFromContentType(conn.getContentType()));
                    Map<String, String> properties = new HashMap<>();
                    List<Event> events = Event.fromReader(reader, properties);

                    info.calendarName = properties.get(Event.CALENDAR_NAME);
                    info.eventsFound = events.size();
                }

            } catch (IOException|InvalidCalendarException e) {
                info.exception = e;
            }
            return info;
        }
    }
}
