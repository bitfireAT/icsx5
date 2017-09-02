/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import at.bitfire.icsdroid.R;
import yuku.ambilwarna.AmbilWarnaDialog;

public class TitleColorFragment extends Fragment implements TextWatcher {
    public static final String
            ARG_URL = "url",
            ARG_TITLE = "title",
            ARG_COLOR = "color";

    private String url;

    private EditText editTitle;
    String title;

    private ColorButton colorButton;
    int color = 0xff2F80C7;

    private OnChangeListener listener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
        View v = inflater.inflate(R.layout.calendar_title_color, container, false);

        if (inState == null) {
            Bundle args = getArguments();
            url = args.getString(ARG_URL);
            title = args.getString(ARG_TITLE);
            color = args.getInt(ARG_COLOR);
        } else {
            url = inState.getString(ARG_URL);
            title = inState.getString(ARG_TITLE);
            color = inState.getInt(ARG_COLOR);
        }

        TextView textURL = (TextView)v.findViewById(R.id.url);
        textURL.setText(url);

        editTitle = (EditText) v.findViewById(R.id.title);
        setTitle(title);
        editTitle.addTextChangedListener(this);

        colorButton = (ColorButton) v.findViewById(R.id.color);
        colorButton.setColor(color);
        colorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AmbilWarnaDialog(getActivity(), color, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onCancel(AmbilWarnaDialog ambilWarnaDialog) {
                    }

                    @Override
                    public void onOk(AmbilWarnaDialog ambilWarnaDialog, int newColor) {
                        colorButton.setColor(color = 0xff000000 | newColor);
                        notifyListener();
                    }
                }).show();
            }
        });

        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_URL, url);
        outState.putString(ARG_TITLE, title);
        outState.putInt(ARG_COLOR, color);
    }


    public void setTitle(String title) {
        editTitle.setText(this.title = title);
    }


    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        title = editTitle.getText().toString();
        notifyListener();
    }


    public interface OnChangeListener {
        void onChangeTitleColor(String title, int color);
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        listener = onChangeListener;
    }

    void notifyListener() {
        if (listener != null)
            listener.onChangeTitleColor(title, color);
    }
}
