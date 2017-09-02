/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui;

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

import at.bitfire.icsdroid.Constants;
import at.bitfire.icsdroid.R;

public class AddCalendarEnterUrlFragment extends Fragment implements TextWatcher, CredentialsFragment.OnCredentialsChangeListener {
    private CredentialsFragment credentials;

    private EditText editURL;
    private TextView insecureAuthWarning;

    private ResourceInfo info = new ResourceInfo();
    private static final String KEY_INFO = "info";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            info = (ResourceInfo)savedInstanceState.getSerializable(KEY_INFO);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_INFO, info);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.add_calendar_enter_url, container, false);
        setHasOptionsMenu(true);

        credentials = new CredentialsFragment();
        credentials.setOnChangeListener(this);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.credentials, credentials)
                .commit();

        insecureAuthWarning = (TextView)v.findViewById(R.id.insecure_authentication_warning);

        editURL = (EditText)v.findViewById(R.id.url);
        editURL.addTextChangedListener(this);
        Uri createUri = getActivity().getIntent().getData();
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
        boolean urlOK = info.url != null,
                authOK = !info.authRequired || (StringUtils.isNotBlank(info.username) && StringUtils.isNotBlank(info.password));
        itemNext.setEnabled(urlOK && authOK);
    }


    /* dynamic changes */

    @Override
    public void onChangeCredentials(boolean authRequired, String username, String password) {
        info.authRequired = authRequired;
        info.username = username;
        info.password = password;
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

        info.url = null;
        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            if ("file".equals(protocol) && StringUtils.isNotEmpty(url.getPath())) {
                info.url = url;
                credentials.authRequired = false;
                getChildFragmentManager().beginTransaction()
                        .hide(credentials)
                        .commit();
            } else if (("http".equals(protocol) || "https".equals(protocol)) && StringUtils.isNotBlank(url.getAuthority())) {
                info.url = url;
                getChildFragmentManager().beginTransaction()
                        .show(credentials)
                        .commit();
            }
        } catch (MalformedURLException e) {
            Log.d(Constants.TAG, "Invalid URL", e);
        }

        editURL.setTextColor(getResources().getColor(info.url != null ?
                android.support.v7.appcompat.R.color.abc_secondary_text_material_light :
                R.color.redorange));
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateHttpWarning();
        getActivity().invalidateOptionsMenu();
    }

    private void updateHttpWarning() {
        // warn if auth. required and not using HTTPS
        if (info.authRequired && info.url != null)
            insecureAuthWarning.setVisibility("https".equals(info.url.getProtocol()) ? View.GONE : View.VISIBLE);
        else
            insecureAuthWarning.setVisibility(View.GONE);
    }


    /* actions */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.next) {
            AddCalendarValidationFragment frag = AddCalendarValidationFragment.newInstance(info);
            frag.show(getFragmentManager(), "validation");
            return true;
        }
        return false;
    }

}
