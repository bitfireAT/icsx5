/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid

import android.Manifest
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class PermissionUtils(val activity: AppCompatActivity) {

    fun getCalendarPermissionRequestLauncher(): ActivityResultLauncher<Array<String>> {
        return activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.get(Manifest.permission.READ_CALENDAR) == false ||
                permissions.get(Manifest.permission.WRITE_CALENDAR) == false) {
                Toast.makeText(activity, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    }
}
