/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.*
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.ui.dialog.SyncIntervalDialog
import at.bitfire.icsdroid.ui.list.CalendarListItem
import at.bitfire.icsdroid.ui.reusable.ActionCard
import com.google.accompanist.themeadapter.material.MdcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
class CalendarListActivity: AppCompatActivity() {

    companion object {
        /**
         * Set this extra to request calendar permission when the activity starts.
         */
        const val EXTRA_REQUEST_CALENDAR_PERMISSION = "permission"

        const val PRIVACY_POLICY_URL = "https://icsx5.bitfire.at/privacy/"
    }

    private val model by viewModels<SubscriptionsModel>()
    val settings by lazy { Settings(this) }

    /** Stores the calendar permission request for asking for calendar permissions during runtime */
    private lateinit var requestCalendarPermissions: () -> Unit

    /** Stores the post notification permission request for asking for permissions during runtime */
    private lateinit var requestNotificationPermission: () -> Unit

    /** Used for launching EditCalendarActivity and editing a subscription */
    private val editSubscriptionLauncher = registerForActivityResult(EditCalendarActivity.Contract) { result ->
        result.message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the calendar permission request
        requestCalendarPermissions = PermissionUtils.registerCalendarPermissionRequest(this) {
            model.checkSyncSettings()

            SyncWorker.run(this)
        }

        // Register the notifications permission request
        requestNotificationPermission = PermissionUtils.registerNotificationPermissionRequest(this) {
            model.checkSyncSettings()
        }

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
                Scaffold(
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
                                ActionOverflowMenu()
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

        model.checkSyncSettings()
    }

    /* UI components */

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun ActivityContent(paddingValues: PaddingValues) {
        val isRefreshing by model.isRefreshing.observeAsState(initial = true)
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = ::onRefreshRequested
        )

        val subscriptions by model.subscriptions.observeAsState()

        val askForCalendarPermission by model.askForCalendarPermission.observeAsState(false)
        val askForNotificationPermission by model.askForNotificationPermission.observeAsState(false)
        val askForWhitelisting by model.askForWhitelisting.observeAsState(false)

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                // Calendar permission card
                if (askForCalendarPermission) {
                    item(key = "calendar-perm") {
                        ActionCard(
                            title = stringResource(R.string.calendar_permissions_required),
                            message = stringResource(R.string.calendar_permissions_required_text),
                            actionText = stringResource(R.string.permissions_grant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .animateItemPlacement()
                        ) {
                            requestCalendarPermissions()
                        }
                    }
                }

                // Notification permission card
                if (askForNotificationPermission) {
                    item(key = "notification-perm") {
                        ActionCard(
                            title = stringResource(R.string.notification_permissions_required),
                            message = stringResource(R.string.notification_permissions_required_text),
                            actionText = stringResource(R.string.permissions_grant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .animateItemPlacement()
                        ) {
                            requestNotificationPermission()
                        }
                    }
                }

                // Whitelisting card
                if (askForWhitelisting && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    item(key = "battery-whitelisting") {
                        ActionCard(
                            title = stringResource(R.string.calendar_list_battery_whitelist_title),
                            message = stringResource(R.string.calendar_list_battery_whitelist_text, stringResource(R.string.app_name)),
                            actionText = stringResource(R.string.calendar_list_battery_whitelist_open_settings),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .animateItemPlacement()
                        ) {
                            val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        }
                    }
                }

                if (subscriptions?.isEmpty() == true) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.calendar_list_empty_info),
                            style = MaterialTheme.typography.body1,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp, 16.dp, 8.dp, 8.dp)
                                .animateItemPlacement()
                        )
                    }
                }

                items(subscriptions ?: emptyList()) { subscription ->
                    CalendarListItem(subscription = subscription, onClick = {
                        editSubscriptionLauncher.launch(
                            EditCalendarActivity.Data(subscription)
                        )
                    })
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    @Composable
    fun ActionOverflowMenu() {
        val context = LocalContext.current

        var showMenu by remember { mutableStateOf(false) }

        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more))
        }

        var showSyncIntervalDialog by rememberSaveable { mutableStateOf(false) }
        if (showSyncIntervalDialog)
            SyncIntervalDialog(
                currentInterval = AppAccount.syncInterval(this),
                onSetSyncInterval = { seconds ->
                    AppAccount.syncInterval(this, seconds)
                    showSyncIntervalDialog = false

                    model.checkSyncSettings()
                },
                onDismiss = { showSyncIntervalDialog = false }
            )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                onClick = {
                    showMenu = false
                    showSyncIntervalDialog = true
                }
            ) {
                Text(stringResource(R.string.calendar_list_set_sync_interval))
            }
            DropdownMenuItem(
                onClick = {
                    showMenu = false
                    onRefreshRequested()
                }
            ) {
                Text(stringResource(R.string.calendar_list_synchronize))
            }
            DropdownMenuItem(
                onClick =  {
                    showMenu = false
                    onToggleDarkMode()
                }
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
                    showMenu = false
                    UriUtils.launchUri(context, Uri.parse(PRIVACY_POLICY_URL))
                }
            ) {
                Text(stringResource(R.string.calendar_list_privacy_policy))
            }
            DropdownMenuItem(
                onClick = {
                    showMenu = false
                    startActivity(Intent(context, InfoActivity::class.java))
                }
            ) {
                Text(stringResource(R.string.calendar_list_info))
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

        val askForCalendarPermission = MutableLiveData(false)
        val askForNotificationPermission = MutableLiveData(false)

        val askForWhitelisting = MutableLiveData(false)


        /** whether there are running sync workers */
        val isRefreshing = SyncWorker.liveStatus(application).map { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING }
        }

        /** LiveData watching the subscriptions */
        val subscriptions = AppDatabase.getInstance(application)
            .subscriptionsDao()
            .getAllLive()

        init {
            // When initialized, update the ask* fields
            checkSyncSettings()
        }

        /**
         * Performs all the checks necessary, and updates [askForCalendarPermission],
         * [askForNotificationPermission] and [askForWhitelisting] which should be shown to the
         * user through a Snackbar.
         */
        fun checkSyncSettings() = viewModelScope.launch(Dispatchers.IO) {
            val haveNotificationPermission = PermissionUtils.haveNotificationPermission(getApplication())
            askForNotificationPermission.postValue(!haveNotificationPermission)

            val haveCalendarPermission = PermissionUtils.haveCalendarPermissions(getApplication())
            askForCalendarPermission.postValue(!haveCalendarPermission)

            val shouldWhitelistApp = if (Build.VERSION.SDK_INT >= 23) {
                val powerManager = getApplication<Application>().getSystemService<PowerManager>()
                val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)

                val syncInterval = AppAccount.syncInterval(getApplication())

                // If not ignoring battery optimizations, and sync interval is less than a day
                isIgnoringBatteryOptimizations == false && syncInterval != AppAccount.SYNC_INTERVAL_MANUALLY && syncInterval < 86400
            } else {
                // If using Android < 6, this is not necessary
                false
            }
            askForWhitelisting.postValue(shouldWhitelistApp)
        }

    }

}