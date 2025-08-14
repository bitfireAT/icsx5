/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import at.bitfire.icsdroid.PermissionUtils.CALENDAR_PERMISSIONS

object PermissionUtils {

    private val CALENDAR_PERMISSIONS = arrayOf(
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
     * Checks whether the calling app has permission to request notifications. If the device's SDK
     * level is lower than Tiramisu, always returns `true`.
     *
     * @param context  context to check permissions within
     * @return *true* if notification permissions are granted; *false* otherwise
     */
    fun haveNotificationPermission(context: Context) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else
            true

    /**
     * Registers for the result of the request of some permissions.
     * Invoke the returned anonymous function to actually request the permissions.
     *
     * When all requested permissions are granted, [onGranted] is called.
     * When not all requested permissions are granted, a toast is shown.
     *
     * @param permissions The permissions to be requested.
     * @param toastMessage The message to show in a toast if at least one permissions was not granted.
     * @param onGranted What to call when all permissions were granted.
     *
     * @return The request launcher for launching the request.
     */
    @Composable
    private fun rememberPermissionRequest(
        permissions: Array<String>,
        @StringRes toastMessage: Int,
        onGranted: () -> Unit = {},
    ): (() -> Unit) {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            Log.i(Constants.TAG, "Requested permissions: ${permissions.asList()}, got permissions: $permissionsResult")
            if (permissions.all { requestedPermission -> permissionsResult.getOrDefault(requestedPermission, null) == true })
            // all permissions granted
                onGranted()
            else {
                // some permissions missing
                Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show()
            }
        }
        return { launcher.launch(permissions) }
    }

    /**
     * Registers a calendar permission request launcher.
     *
     * @param onGranted  called when calendar permissions have been granted
     *
     * @return Call the returning function to launch the request
     */
    @Composable
    fun rememberCalendarPermissionRequest(onGranted: () -> Unit = {}) =
        rememberPermissionRequest(
            CALENDAR_PERMISSIONS,
            R.string.calendar_permissions_required,
            onGranted
        )

    /**
     * Registers a notification permission request launcher.
     *
     * @param onGranted  called when calendar permissions have been granted
     *
     * @return Call the returning function to launch the request
     */
    @Composable
    fun rememberNotificationPermissionRequest(onGranted: () -> Unit = {}) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            rememberPermissionRequest(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                R.string.notification_permissions_required,
                onGranted
            )
        else {
            // If SDK level is not greater or equal than Tiramisu, do nothing
            {}
        }

}