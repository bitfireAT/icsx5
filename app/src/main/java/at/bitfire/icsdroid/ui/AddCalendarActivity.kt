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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.LocalCalendar

class AddCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_COLOR = "color"
    }

    private val titleColorModel by viewModels<TitleColorFragment.TitleColorModel>()


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.get(Manifest.permission.READ_CALENDAR) == false ||
                permissions.get(Manifest.permission.WRITE_CALENDAR) == false) {
                Toast.makeText(this, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
                finish()
            }
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))

        if (inState == null) {
            supportFragmentManager
                    .beginTransaction()
                    .add(android.R.id.content, AddCalendarEnterUrlFragment())
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

}
