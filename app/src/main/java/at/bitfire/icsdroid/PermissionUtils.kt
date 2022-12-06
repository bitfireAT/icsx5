/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import at.bitfire.icsdroid.ui.NotificationUtils

object PermissionUtils {

    val CALENDAR_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    val NOTIFICATION_PERMISSIONS = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS,
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
     * Checks whether the calling app has all [NOTIFICATION_PERMISSIONS].
     *
     * @param context  context to check permissions within
     * @return *true* if all notification permissions are granted; *false* otherwise
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun haveNotificationPermissions(context: Context) = NOTIFICATION_PERMISSIONS.all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Registers for the result of the request of some permissions.
     * @since 20221206
     * @param activity The activity where to register the request launcher.
     * @param permissions All the permissions to be requested.
     * @param toastMessage The message to show in a toast if the permissions are not granted.
     * @param onGranted What to call when the permissions are granted.
     * @return The request launcher for launching the request.
     */
    private fun registerPermissionRequest(
        activity: AppCompatActivity,
        permissions: Array<String>,
        @StringRes toastMessage: Int,
        onGranted: () -> Unit,
    ): (() -> Unit) {
        val request = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            if (permissionsResult.any { (k, v) -> !permissions.contains(k) || !v }) {
                // some permissions missing
                Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
                activity.finish()
            } else if (permissionsResult.all { (_, v) -> v }) {
                // we have all the permissions, cancel possible notification
                val nm = NotificationManagerCompat.from(activity)
                nm.cancel(NotificationUtils.NOTIFY_PERMISSION)

                onGranted()
            }
        }
        return { request.launch(permissions) }
    }

    /**
     * Registers a calendar permission request launcher.
     *
     * @param activity   activity to register permission request launcher
     * @param onGranted  called when calendar permissions have been granted
     *
     * @return Call the returning function to launch the request
     */
    fun registerCalendarPermissionRequest(activity: AppCompatActivity, onGranted: () -> Unit = {}) =
        registerPermissionRequest(
            activity,
            CALENDAR_PERMISSIONS,
            R.string.calendar_permissions_required,
            onGranted,
        )


    /**
     * Registers a notifications permission request launcher.
     *
     * @param activity   activity to register permission request launcher
     * @param onGranted  called when calendar permissions have been granted
     *
     * @return Call the returning function to launch the request
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun registerNotificationsPermissionRequest(activity: AppCompatActivity, onGranted: () -> Unit = {}) =
        registerPermissionRequest(
            activity,
            NOTIFICATION_PERMISSIONS,
            R.string.calendar_permissions_required,
            onGranted,
        )

}
