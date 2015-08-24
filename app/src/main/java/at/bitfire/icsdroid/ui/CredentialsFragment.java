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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import at.bitfire.icsdroid.R;

public class CredentialsFragment extends Fragment implements CompoundButton.OnCheckedChangeListener, TextWatcher {
    public static final String
            ARG_AUTH_REQUIRED = "auth_required",
            ARG_USERNAME = "username",
            ARG_PASSWORD = "password";

    boolean authRequired;
    String username, password;

    OnCredentialsChangeListener onChangeListener;

    Switch switchAuthRequired;
    EditText editUsername, editPassword;
    TextView textUsername, textPassword;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.credentials, container, false);

        if (savedInstanceState == null && getArguments() != null) {
            Bundle args = getArguments();
            authRequired = args.getBoolean(ARG_AUTH_REQUIRED);
            username = args.getString(ARG_USERNAME);
            password = args.getString(ARG_PASSWORD);
        } else if (savedInstanceState != null) {
            authRequired = savedInstanceState.getBoolean(ARG_AUTH_REQUIRED);
            username = savedInstanceState.getString(ARG_USERNAME);
            password = savedInstanceState.getString(ARG_PASSWORD);
        }

        switchAuthRequired = (Switch)v.findViewById(R.id.requires_authentication);
        switchAuthRequired.setChecked(authRequired);
        switchAuthRequired.setOnCheckedChangeListener(this);

        textUsername = (TextView)v.findViewById(R.id.user_name_label);
        editUsername = (EditText)v.findViewById(R.id.user_name);
        editUsername.setText(username);
        editUsername.addTextChangedListener(this);

        textPassword = (TextView)v.findViewById(R.id.password_label);
        editPassword = (EditText)v.findViewById(R.id.password);
        editPassword.setText(password);
        editPassword.addTextChangedListener(this);

        updateViews();
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_AUTH_REQUIRED, authRequired);
        outState.putString(ARG_USERNAME, username);
        outState.putString(ARG_PASSWORD, password);
    }


    public interface OnCredentialsChangeListener {
        void onChangeCredentials(boolean authRequired, String username, String password);
    }

    public void setOnChangeListener(OnCredentialsChangeListener listener) {
        onChangeListener = listener;
    }

    void notifyListener() {
        if (onChangeListener != null)
            onChangeListener.onChangeCredentials(authRequired, authRequired ? username : null, authRequired ? password : null);
    }

    void updateViews() {
        if (authRequired) {
            textUsername.setVisibility(View.VISIBLE);
            editUsername.setVisibility(View.VISIBLE);
            textPassword.setVisibility(View.VISIBLE);
            editPassword.setVisibility(View.VISIBLE);
        } else {
            textUsername.setVisibility(View.GONE);
            editUsername.setVisibility(View.GONE);
            textPassword.setVisibility(View.GONE);
            editPassword.setVisibility(View.GONE);
        }

    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        authRequired = isChecked;
        updateViews();
        notifyListener();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        username = editUsername.getText().toString();
        password = editPassword.getText().toString();
        notifyListener();
    }

}
