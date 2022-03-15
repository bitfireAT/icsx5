/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.Manifest
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import at.bitfire.icsdroid.ui.NotificationUtils

class PermissionUtils(val activity: AppCompatActivity) {

    fun registerCalendarPermissionRequestLauncher() =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.READ_CALENDAR] == false ||
                permissions[Manifest.permission.WRITE_CALENDAR] == false) {
                // calendar permissions missing
                Toast.makeText(activity, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
                activity.finish()

            } else if (permissions[Manifest.permission.READ_CALENDAR] == true &&
                       permissions[Manifest.permission.WRITE_CALENDAR] == true) {
                // we have calendar permissions, cancel possible notification
                val nm = NotificationManagerCompat.from(activity)
                nm.cancel(NotificationUtils.NOTIFY_PERMISSION)
            }
        }

}
