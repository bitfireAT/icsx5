/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.map
import androidx.work.WorkInfo
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.ui.dialog.SyncIntervalDialog
import at.bitfire.icsdroid.ui.list.CalendarListItem
import com.google.accompanist.themeadapter.material.MdcTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class CalendarListActivity: AppCompatActivity() {

    companion object {
        /**
         * Set this extra to request calendar permission when the activity starts.
         */
        const val EXTRA_REQUEST_CALENDAR_PERMISSION = "permission"

        const val PRIVACY_POLICY_URL = "https://icsx5.bitfire.at/privacy/"
    }

    private val model by viewModels<SubscriptionsModel>()

    /** Stores the calendar permission request for asking for calendar permissions during runtime */
    private lateinit var requestCalendarPermissions: () -> Unit

    /** Stores the post notification permission request for asking for permissions during runtime */
    private lateinit var requestNotificationPermission: () -> Unit

    private val snackBarHostState: SnackbarHostState by lazy { SnackbarHostState() }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the calendar permission request
        requestCalendarPermissions = PermissionUtils.registerCalendarPermissionRequest(this) {
            SyncWorker.run(this)
        }

        // Register the notifications permission request
        requestNotificationPermission = PermissionUtils.registerNotificationPermissionRequest(this)

        // If EXTRA_PERMISSION is true, request the calendar permissions
        val requestPermissions = intent.getBooleanExtra(EXTRA_REQUEST_CALENDAR_PERMISSION, false)
        if (requestPermissions && !PermissionUtils.haveCalendarPermissions(this))
            requestCalendarPermissions()

        // startup fragments
        if (savedInstanceState == null)
            ServiceLoader
                .load(StartupFragment::class.java)
                .forEach { it.initialize(this) }

        setContent {
            MdcTheme {
                val snackbarHostState = remember { snackBarHostState }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                // Launch the Subscription add Activity
                                startActivity(Intent(this, AddCalendarActivity::class.java))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.activity_add_calendar)
                            )
                        }
                    },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(stringResource(R.string.title_activity_calendar_list))
                            },
                            actions = {
                                TopBarDropdown()
                            }
                        )
                    }
                ) { paddingValues ->
                    ActivityContent(paddingValues)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        CoroutineScope(Dispatchers.IO).launch {
            checkSyncSettings()
        }
    }

    /* UI components */

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ActivityContent(paddingValues: PaddingValues) {
        val context = LocalContext.current

        val isRefreshing by model.isRefreshing.observeAsState(initial = true)
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = ::onRefreshRequested
        )

        val subscriptions by model.subscriptions.observeAsState()

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(subscriptions ?: emptyList()) { subscription ->
                    CalendarListItem(subscription = subscription, onClick = {
                        val intent = Intent(context, EditCalendarActivity::class.java)
                        intent.putExtra(EditCalendarActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
                        startActivity(intent)
                    })
                }
            }

            AnimatedVisibility(
                visible = subscriptions?.isEmpty() == true,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.calendar_list_empty_info),
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center
                )
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    @Composable
    fun TopBarDropdown() {
        val context = LocalContext.current
        val settings = remember { Settings(context) }
        var expanded by remember { mutableStateOf(false) }

        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more))
        }

        var showSyncIntervalDialog by remember { mutableStateOf(false) }
        if (showSyncIntervalDialog)
            SyncIntervalDialog(
                currentInterval = AppAccount.syncInterval(this),
                onSetSyncInterval = { seconds ->
                    AppAccount.syncInterval(this, seconds)
                    showSyncIntervalDialog = false

                    CoroutineScope(Dispatchers.IO).launch { checkSyncSettings() }
                }
            ) { showSyncIntervalDialog = false }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                onClick = { showSyncIntervalDialog = true }
            ) {
                Text(stringResource(R.string.calendar_list_set_sync_interval))
            }
            DropdownMenuItem(
                onClick = ::onRefreshRequested
            ) {
                Text(stringResource(R.string.calendar_list_synchronize))
            }
            DropdownMenuItem(
                onClick = ::onToggleDarkMode
            ) {
                val forceDarkMode by settings.forceDarkModeLive().observeAsState(false)

                Text(stringResource(R.string.settings_force_dark_theme))
                Checkbox(
                    checked = forceDarkMode,
                    onCheckedChange = { onToggleDarkMode() }
                )
            }
            DropdownMenuItem(
                onClick = {
                    UriUtils.launchUri(context, Uri.parse(PRIVACY_POLICY_URL))
                }
            ) {
                Text(stringResource(R.string.calendar_list_privacy_policy))
            }
            DropdownMenuItem(
                onClick = {
                    startActivity(Intent(context, InfoActivity::class.java))
                }
            ) {
                Text(stringResource(R.string.calendar_list_info))
            }
        }
    }


    /**
     * Checks the current settings and permissions state, and shows a Snackbar using
     * [snackBarHostState] if any action is necessary.
     *
     * Blocks the current thread until the Snackbar is hidden, or the action to be performed is
     * completed.
     */
    private suspend fun checkSyncSettings() {
        when {
            // notification permissions are granted
            !PermissionUtils.haveNotificationPermission(this) -> {
                val response = snackBarHostState.showSnackbar(
                    message = getString(R.string.notification_permissions_required),
                    actionLabel = getString(R.string.permissions_grant),
                    duration = SnackbarDuration.Indefinite
                )
                if (response == SnackbarResult.ActionPerformed)
                    requestNotificationPermission()
            }

            // calendar permissions are granted
            !PermissionUtils.haveCalendarPermissions(this) -> {
                val response = snackBarHostState.showSnackbar(
                    message = getString(R.string.calendar_permissions_required),
                    actionLabel = getString(R.string.permissions_grant),
                    duration = SnackbarDuration.Indefinite
                )
                if (response == SnackbarResult.ActionPerformed)
                    requestCalendarPermissions()
            }

            // periodic sync enabled AND Android >= 6 AND not whitelisted from battery saving AND sync interval < 1 day
            Build.VERSION.SDK_INT >= 23 &&
                    !(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
                    AppAccount.syncInterval(this) < 86400 -> {
                val response = snackBarHostState.showSnackbar(
                    message = getString(R.string.calendar_list_battery_whitelist),
                    actionLabel = getString(R.string.permissions_grant),
                    duration = SnackbarDuration.Indefinite
                )
                if (response == SnackbarResult.ActionPerformed) {
                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    startActivity(intent)
                }
            }
        }
    }


    /* actions */

    private fun onRefreshRequested() {
        SyncWorker.run(this, true)
    }

    private fun onToggleDarkMode() {
        val settings = Settings(this)
        val newMode = !settings.forceDarkMode()

        settings.forceDarkMode(newMode)
    }


    class SubscriptionsModel(application: Application): AndroidViewModel(application) {

        /** whether there are running sync workers */
        val isRefreshing = SyncWorker.liveStatus(application).map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING }
        }

        /** LiveData watching the subscriptions */
        val subscriptions = AppDatabase.getInstance(application)
            .subscriptionsDao()
            .getAllLive()

    }

}