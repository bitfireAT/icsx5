/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import at.bitfire.icsdroid.ui.NotificationUtils

object PermissionUtils {

    val CALENDAR_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    /**
     * Checks whether the calling app has all [CALENDAR_PERMISSIONS].
     *
     * @param context  context to check permissions within
     * @return *true* if all calendar permissions are granted; *false* otherwise
     */
    fun haveCalendarPermissions(context: Context) = CALENDAR_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Registers a calendar permission request launcher.
     *
     * @param activity   activity to register permission request launcher
     * @param onGranted  called when calendar permissions have been granted
     *
     * @return permission request launcher; has to be called with `launch(PermissionUtils.CALENDAR_PERMISSIONS)`
     */
    fun registerCalendarPermissionRequest(activity: AppCompatActivity, onGranted: () -> Unit = {}) =
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

                onGranted()
            }
        }

}
