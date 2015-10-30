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

package at.bitfire.icsdroid.ui;

import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.CalendarContract;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.BuildConfig;
import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;

public class CalendarListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<LocalCalendar[]>, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, SyncStatusObserver {
    private static final String TAG = "icsdroid.CalendarList";
    private final String DATA_SYNC_ACTIVE = "sync_active";

    Object syncStatusHandle;
    Handler syncStatusHandler;

    ListView list;
    CalendarListAdapter listAdapter;

    SwipeRefreshLayout refresher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_calendar_list);
        setContentView(R.layout.calendar_list_activity);

        refresher = (SwipeRefreshLayout)findViewById(R.id.refresh);
        refresher.setColorSchemeColors(getResources().getColor(R.color.lightblue));
        refresher.setOnRefreshListener(this);

        refresher.setSize(SwipeRefreshLayout.LARGE);

        list = (ListView)findViewById(R.id.calendar_list);
        list.setAdapter(listAdapter = new CalendarListAdapter(this));
        list.setOnItemClickListener(this);

        AppAccount.makeAvailable(this);

        String installer = getPackageManager().getInstallerPackageName(BuildConfig.APPLICATION_ID);
        if (installer == null || installer.startsWith("org.fdroid"))
            new DonateDialogFragment().show(getSupportFragmentManager(), "donate");

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_calendar_list, menu);
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LocalCalendar calendar = (LocalCalendar)parent.getItemAtPosition(position);

        Intent i = new Intent(this, EditCalendarActivity.class);
        i.setData(ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendar.getId()));
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        syncStatusHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                final boolean syncActive = AppAccount.isSyncActive(CalendarListActivity.this);
                Log.d(CalendarListActivity.TAG, "Is sync. active? " + (syncActive ? "yes" : "no"));
                // workaround: see https://code.google.com/p/android/issues/detail?id=77712
                refresher.post(new Runnable() {
                    @Override
                    public void run() {
                        refresher.setRefreshing(syncActive);
                    }
                });
                return true;
            }
        });
        syncStatusHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this);
        syncStatusHandler.sendEmptyMessage(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (syncStatusHandle != null)
            ContentResolver.removeStatusChangeListener(syncStatusHandle);
    }


    /* (sync) status changes */

    @Override
    public void onStatusChanged(int which) {
        if (which == ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE && syncStatusHandler != null)
            syncStatusHandler.sendEmptyMessage(0);
    }


    /* loader callbacks */

    @Override
    public Loader<LocalCalendar[]> onCreateLoader(int id, Bundle args) {
        return new CalendarListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<LocalCalendar[]> loader, LocalCalendar[] calendars) {
        // we got our list of calendars

        // add them into the list
        listAdapter.clear();
        listAdapter.addAll(calendars);

        // control the swipe refresher
        if (calendars.length >= 1) {
            // funny: use the calendar colors for the sync status
            int colors[] = new int[calendars.length];
            int idx = 0;
            for (LocalCalendar calendar : calendars)
                colors[idx++] = 0xff000000 | calendar.getColor();
            refresher.setColorSchemeColors(colors);
        }
    }

    @Override
    public void onLoaderReset(Loader<LocalCalendar[]> loader) {
    }


    /* actions */

    public void onAddCalendar(View v) {
        startActivity(new Intent(this, AddCalendarActivity.class));
    }

    public void onDonate(MenuItem item) {
        startActivity(new Intent(Intent.ACTION_VIEW, Constants.donationUri));
    }

    @Override
    public void onRefresh() {
        Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_DO_NOT_RETRY, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, true);
        getContentResolver().requestSync(AppAccount.account, CalendarContract.AUTHORITY, extras);
    }

    public void onShowInfo(MenuItem item) {
        startActivity(new Intent(this, InfoActivity.class));
    }

    public void onSetSyncInterval(MenuItem item) {
        DialogFragment frag = SyncIntervalDialogFragment.newInstance();
        frag.show(getFragmentManager(), "sync_interval");
    }


    /* list adapter */

    public static class CalendarListAdapter extends ArrayAdapter<LocalCalendar> {
        private static final String TAG = "ICSdroid.CalendarList";

        final Context context;

        public CalendarListAdapter(Context context) {
            super(context, R.layout.calendar_list_item);
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null)
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.calendar_list_item, parent, false);

            LocalCalendar calendar = getItem(position);
            ((TextView) v.findViewById(R.id.url)).setText(calendar.getUrl());
            ((TextView) v.findViewById(R.id.title)).setText(calendar.getDisplayName());

            String syncStatus;
            if (!calendar.isSynced())
                syncStatus = context.getString(R.string.calendar_list_sync_disabled);
            else {
                DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
                syncStatus = calendar.getLastSync() == 0 ?
                        context.getString(R.string.calendar_list_not_synced_yet) :
                        formatter.format(new Date(calendar.getLastSync()));
            }
            ((TextView) v.findViewById(R.id.sync_status)).setText(syncStatus);

            ((ColorButton) v.findViewById(R.id.color)).setColor(calendar.getColor().intValue());

            String errorMessage = calendar.getErrorMessage();
            TextView textError = (TextView) v.findViewById(R.id.error_message);
            if (errorMessage == null)
                textError.setVisibility(View.INVISIBLE);
            else {
                textError.setText(errorMessage);
                textError.setVisibility(View.VISIBLE);
            }

            return v;
        }
    }

    protected static class CalendarListLoader extends Loader<LocalCalendar[]> {
        private static final String TAG = "ICSdroid.CalendarsLoad";

        ContentProviderClient provider;
        ContentObserver observer;

        public CalendarListLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            ContentResolver resolver = getContext().getContentResolver();
            provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY);

            observer = new ForceLoadContentObserver();
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, false, observer);

            forceLoad();
        }

        @Override
        protected void onStopLoading() {
            getContext().getContentResolver().unregisterContentObserver(observer);
            if (provider != null)
                provider.release();
        }

        @Override
        public void onForceLoad() {
            try {
                LocalCalendar[] calendars;
                if (provider != null)
                    calendars = LocalCalendar.findAll(AppAccount.account, provider);
                else
                    calendars = LocalCalendar.Factory.FACTORY.newArray(0);
                deliverResult(calendars);
            } catch (CalendarStorageException e) {
                Log.e(TAG, "Couldn't load calendar list", e);
            }
        }

    }

}
