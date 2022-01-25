/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

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
