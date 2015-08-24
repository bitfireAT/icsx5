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

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.apache.commons.lang3.StringUtils;
import android.provider.CalendarContract.Calendars;
import android.widget.Toast;

import at.bitfire.ical4android.AndroidCalendar;
import at.bitfire.ical4android.CalendarStorageException;
import at.bitfire.icsdroid.AppAccount;
import at.bitfire.icsdroid.R;
import at.bitfire.icsdroid.db.LocalCalendar;

public class AddCalendarDetailsFragment extends Fragment implements TitleColorFragment.OnChangeListener {
    private static final String
            TAG = "ICSdroid.CreateCalendar",
            STATE_TITLE = "title",
            STATE_COLOR = "color";

    String title;
    int color = 0xff2F80C7;

    AddCalendarActivity activity;
    TitleColorFragment fragTitleColor;

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        activity = (AddCalendarActivity)context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
        View v = inflater.inflate(R.layout.add_calendar_details, container, false);
        setHasOptionsMenu(true);

        if (inState != null) {
            title = inState.getString(STATE_TITLE);
            color = inState.getInt(STATE_COLOR);
        } else {
            String path = activity.url.getPath();
            title = path.substring(path.lastIndexOf('/') + 1);
        }

        fragTitleColor = new TitleColorFragment();
        Bundle args = new Bundle(3);
        args.putString(TitleColorFragment.ARG_URL, activity.url.toString());
        args.putString(TitleColorFragment.ARG_TITLE, title);
        args.putInt(TitleColorFragment.ARG_COLOR, color);
        fragTitleColor.setArguments(args);
        fragTitleColor.setOnChangeListener(this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.title_color, fragTitleColor)
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
                activity.finish();
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
        calInfo.put(Calendars.NAME, activity.url.toString());
        calInfo.put(Calendars.CALENDAR_DISPLAY_NAME, title);
        calInfo.put(Calendars.CALENDAR_COLOR, color);
        calInfo.put(Calendars.OWNER_ACCOUNT, AppAccount.account.name);
        calInfo.put(Calendars.SYNC_EVENTS, 1);
        calInfo.put(Calendars.VISIBLE, 1);
        calInfo.put(LocalCalendar.COLUMN_USERNAME, activity.authRequired ? activity.username : null);
        calInfo.put(LocalCalendar.COLUMN_PASSWORD, activity.authRequired ? activity.password : null);
        calInfo.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
        try {
            AndroidCalendar.create(AppAccount.account, activity.getContentResolver(), calInfo);
            Toast.makeText(activity, getString(R.string.add_calendar_created), Toast.LENGTH_LONG).show();
            activity.invalidateOptionsMenu();
            return true;
        } catch (CalendarStorageException e) {
            Log.e(TAG, "Couldn't create calendar", e);
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
}
