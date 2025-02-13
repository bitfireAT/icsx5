/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.model.SubscriptionsModel
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.ui.InfoActivity
import at.bitfire.icsdroid.ui.partials.AlertDialog
import at.bitfire.icsdroid.ui.screen.CalendarListScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed
import at.bitfire.icsdroid.worker.BaseSyncWorker
import java.util.ServiceLoader

class CalendarListActivity: AppCompatActivity() {

    companion object {
        /**
         * Set this extra to request calendar permission when the activity starts.
         */
        const val EXTRA_REQUEST_CALENDAR_PERMISSION = "permission"

        const val PRIVACY_POLICY_URL = "https://icsx5.bitfire.at/privacy/"

        /**
         * If set, an alert dialog will be displayed with the message of this error.
         * May be set together with [EXTRA_THROWABLE] to display a stack trace.
         */
        const val EXTRA_ERROR_MESSAGE = "errorMessage"

        /**
         * If set, an alert dialog will be displayed with the stack trace of this error.
         * If set, [EXTRA_ERROR_MESSAGE] must also be set, otherwise does nothing.
         */
        const val EXTRA_THROWABLE = "errorThrowable"
    }

    private val model by viewModels<SubscriptionsModel>()

    /** Stores the calendar permission request for asking for calendar permissions during runtime */
    private lateinit var requestCalendarPermissions: () -> Unit

    /** Stores the post notification permission request for asking for permissions during runtime */
    private lateinit var requestNotificationPermission: () -> Unit


    override fun onCreate(savedInstanceState: Bundle?) {
        configureEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Register the calendar permission request
        requestCalendarPermissions = PermissionUtils.registerCalendarPermissionRequest(this) {
            model.checkSyncSettings()

            BaseSyncWorker.run(this)
        }

        // Register the notifications permission request
        requestNotificationPermission = PermissionUtils.registerNotificationPermissionRequest(this) {
            model.checkSyncSettings()
        }

        // If EXTRA_PERMISSION is true, request the calendar permissions
        val requestPermissions = intent.getBooleanExtra(EXTRA_REQUEST_CALENDAR_PERMISSION, false)
        if (requestPermissions && !PermissionUtils.haveCalendarPermissions(this))
            requestCalendarPermissions()

        // Init and collect all ComposableStartupServices
        val compStartupServices = ServiceLoader.load(ComposableStartupService::class.java)

        setContentThemed {
            compStartupServices.forEach { service ->
                val show: Boolean by service.shouldShow()
                if (show) service.Content()
            }

            // show error message from calling intent, if available
            var showingErrorMessage by remember {
                mutableStateOf(savedInstanceState == null && intent.hasExtra(EXTRA_ERROR_MESSAGE))
            }
            if (showingErrorMessage) {
                AlertDialog(
                    intent.getStringExtra(EXTRA_ERROR_MESSAGE)!!,
                    intent.getSerializableExtra(EXTRA_THROWABLE) as? Throwable
                ) { showingErrorMessage = false }
            }

            CalendarListScreen(
                model = model,
                onAboutRequested = {
                    startActivity(Intent(this, InfoActivity::class.java))
                },
                onAddRequested = {
                    startActivity(Intent(this, AddCalendarActivity::class.java))
                },
                onRequestCalendarPermissions = requestCalendarPermissions,
                onRequestNotificationPermission = requestNotificationPermission,
                onItemSelected = { subscription ->
                    val intent = Intent(this, EditCalendarActivity::class.java)
                    intent.putExtra(EditCalendarActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
                    startActivity(intent)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        model.checkSyncSettings()
    }

}