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

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;
import lombok.Cleanup;

public class AddCalendarDetailsFragment extends Fragment implements TitleColorFragment.OnChangeListener {
    private static final String
            STATE_TITLE = "title",
            STATE_COLOR = "color";
    public static final String ARG_INFO = "info";

    ResourceInfo info;

    String title;
    int color = 0xff2F80C7;

    TitleColorFragment fragTitleColor;

    public static AddCalendarDetailsFragment newInstance(@NonNull ResourceInfo info) {
        AddCalendarDetailsFragment frag = new AddCalendarDetailsFragment();
        Bundle args = new Bundle(1);
        args.putSerializable(ARG_INFO, info);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        info = (ResourceInfo)getArguments().getSerializable(ARG_INFO);
        if (info == null)
            throw new IllegalArgumentException();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
        View v = inflater.inflate(R.layout.add_calendar_details, container, false);
        setHasOptionsMenu(true);

        if (inState != null) {
            title = inState.getString(STATE_TITLE);
            color = inState.getInt(STATE_COLOR);
        } else
            title = info.calendarName;

        fragTitleColor = new TitleColorFragment();
        Bundle args = new Bundle(3);
        args.putString(TitleColorFragment.ARG_URL, info.url.toString());
        args.putString(TitleColorFragment.ARG_TITLE, title);
        args.putInt(TitleColorFragment.ARG_COLOR, color);
        fragTitleColor.setArguments(args);
        fragTitleColor.setOnChangeListener(this);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.title_color, fragTitleColor)
                .commit();
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_TITLE, title);
        outState.putInt(STATE_COLOR, color);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_create_calendar, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem itemGo = menu.findItem(R.id.create_calendar);
        itemGo.setEnabled(StringUtils.isNotBlank(title));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.create_calendar) {
            if (createCalendar())
                getActivity().finish();
            return true;
        }
        return false;
    }


    @Override
    public void onChangeTitleColor(String title, int color) {
        this.title = title;
        this.color = color;

        getActivity().invalidateOptionsMenu();
    }


    private boolean createCalendar() {
        AppAccount.makeAvailable(getContext());

        ContentValues calInfo = new ContentValues();
        calInfo.put(Calendars.ACCOUNT_NAME, AppAccount.account.name);
        calInfo.put(Calendars.ACCOUNT_TYPE, AppAccount.account.type);
        calInfo.put(Calendars.NAME, info.url.toString());
        calInfo.put(Calendars.CALENDAR_DISPLAY_NAME, title);
        calInfo.put(Calendars.CALENDAR_COLOR, color);
        calInfo.put(Calendars.OWNER_ACCOUNT, AppAccount.account.name);
        calInfo.put(Calendars.SYNC_EVENTS, 1);
        calInfo.put(Calendars.VISIBLE, 1);
        calInfo.put(LocalCalendar.COLUMN_USERNAME, info.authRequired ? info.username : null);
        calInfo.put(LocalCalendar.COLUMN_PASSWORD, info.authRequired ? info.password : null);
        calInfo.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
        try {
            @Cleanup("release") ContentProviderClient client = getContext().getContentResolver().acquireContentProviderClient(CalendarContract.AUTHORITY);
		    if (client == null)
			    throw new CalendarStorageException("No calendar provider found (calendar storage disabled?)");

            AndroidCalendar.create(AppAccount.account, client, calInfo);
            Toast.makeText(getContext(), getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show();
            getActivity().invalidateOptionsMenu();
            return true;
        } catch (CalendarStorageException e) {
            Log.e(Constants.TAG, "Couldn't create calendar", e);
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
