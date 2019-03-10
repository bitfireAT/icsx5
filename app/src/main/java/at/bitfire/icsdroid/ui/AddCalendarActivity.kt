/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.LocalCalendar

class AddCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private lateinit var titleColorModel: TitleColorFragment.TitleColorModel

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.fragment_container)

        titleColorModel = ViewModelProviders.of(this).get(TitleColorFragment.TitleColorModel::class.java)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 0)

        if (inState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, AddCalendarEnterUrlFragment())
                    .commit()

            intent?.apply {
                data?.let { uri ->
                    titleColorModel.url.value = uri.toString()
                }
                getStringExtra(EXTRA_TITLE)?.let {
                    titleColorModel.title.value = it
                }
                if (hasExtra(EXTRA_COLOR))
                    titleColorModel.color.value = getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissions.forEachIndexed { idx, perm ->
            if (grantResults[idx] != PackageManager.PERMISSION_GRANTED)
                when (perm) {
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR ->
                        finish()
                }
        }
    }

}
