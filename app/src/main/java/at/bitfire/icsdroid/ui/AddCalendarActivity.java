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

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import at.bitfire.icsdroid.R;

public class AddCalendarActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        setContentView(R.layout.fragment_container);

        if (inState == null)
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, new AddCalendarEnterUrlFragment())
                    .commit();
    }

}
