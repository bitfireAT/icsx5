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

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;

import at.bitfire.icsdroid.R;

public class AddCalendarEnterUrlFragment extends Fragment implements TextWatcher, CredentialsFragment.OnCredentialsChangeListener {
    private static final String TAG = "ICSdroid.EnterUrl";

    AddCalendarActivity activity;
    CredentialsFragment credentials;

    EditText editURL;
    TextView insecureAuthWarning;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (AddCalendarActivity)activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.add_calendar_enter_url, container, false);
        setHasOptionsMenu(true);

        credentials = new CredentialsFragment();
        credentials.setOnChangeListener(this);
        getChildFragmentManager().beginTransaction()
                .add(R.id.credentials, credentials)
                .commit();

        insecureAuthWarning = (TextView)v.findViewById(R.id.insecure_authentication_warning);

        editURL = (EditText)v.findViewById(R.id.url);
        Uri createUri = getActivity().getIntent().getData();
        editURL.addTextChangedListener(this);
        if (createUri != null)
            // This causes the onTextChanged listeners to be activated - credentials and insecureAuthWarning are already required!
            editURL.setText(createUri.toString());

        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.enter_url_fragment, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem itemNext = menu.findItem(R.id.next);
        boolean urlOK = activity.url != null,
                authOK = !credentials.authRequired || (StringUtils.isNotBlank(credentials.username) && StringUtils.isNotBlank(credentials.password));
        itemNext.setEnabled(urlOK && authOK);
    }


    /* dynamic changes */

    @Override
    public void onChangeCredentials(boolean authRequired, String username, String password) {
        activity.authRequired = authRequired;
        activity.username = username;
        activity.password = password;
        updateHttpWarning();
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String urlString = editURL.getText().toString();
        if (urlString.startsWith("webcal://") || urlString.startsWith("webcals://")) {
            urlString = "http" + urlString.substring(6);
            editURL.setText(urlString);
        }

        activity.url = null;
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if ("file".equals(protocol) || ("http".equals(protocol) || "https".equals(protocol)) && StringUtils.isNotBlank(url.getAuthority()))
                activity.url = url;
        } catch (MalformedURLException e) {
            Log.d(TAG, "Invalid URL", e);
        }

        editURL.setTextColor(getResources().getColor(activity.url != null ?
                R.color.secondary_text_default_material_light :
                R.color.redorange));
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateHttpWarning();
        getActivity().invalidateOptionsMenu();
    }

    void updateHttpWarning() {
        // warn if auth. required and not using HTTPS
        if (credentials.authRequired && activity.url != null)
            insecureAuthWarning.setVisibility("https".equals(activity.url.getProtocol()) ? View.GONE : View.VISIBLE);
        else
            insecureAuthWarning.setVisibility(View.GONE);
    }


    /* actions */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.next) {
            AddCalendarValidationFragment frag = new AddCalendarValidationFragment();
            frag.show(getFragmentManager(), "validation");
            return true;
        }
        return false;
    }

}
