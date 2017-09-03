/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import at.bitfire.icsdroid.R;

class AddCalendarActivity: AppCompatActivity() {

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.fragment_container)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (inState == null)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, AddCalendarEnterUrlFragment())
                    .commit()
    }

}
