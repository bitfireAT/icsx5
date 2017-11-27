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
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import at.bitfire.icsdroid.R

class AddCalendarActivity: AppCompatActivity() {

    companion object {
        val EXTRA_TITLE = "title"
        val EXTRA_COLOR = "color"
    }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.fragment_container)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALENDAR), 0)

        if (inState == null)
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, AddCalendarEnterUrlFragment())
                    .commit()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissions.forEachIndexed { idx, perm ->
            if (grantResults[idx] != PackageManager.PERMISSION_GRANTED)
                when (perm) {
                    Manifest.permission.WRITE_CALENDAR ->
                        finish()
                    Manifest.permission.READ_EXTERNAL_STORAGE ->
                        Toast.makeText(this, R.string.permission_required_external_storage, Toast.LENGTH_SHORT).show()
                }
        }
    }

}
