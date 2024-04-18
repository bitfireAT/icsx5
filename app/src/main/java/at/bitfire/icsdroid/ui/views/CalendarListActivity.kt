/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.Settings
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.UriUtils
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.service.ComposableStartupService
import at.bitfire.icsdroid.ui.InfoActivity
import at.bitfire.icsdroid.ui.partials.ActionCard
import at.bitfire.icsdroid.ui.partials.CalendarListItem
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.SyncIntervalDialog
import at.bitfire.icsdroid.ui.theme.setContentThemed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.ServiceLoader

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


    @OptIn(ExperimentalMaterial3Api::class)
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

        // Init and collect all ComposableStartupServices
        val compStartupServices = ServiceLoader.load(ComposableStartupService::class.java)
            .onEach { it.initialize(this) }

        setContentThemed {
            compStartupServices.forEach { service ->
                val show: Boolean by service.shouldShow()
                if (show) service.Content()
            }

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
                    ExtendedTopAppBar(
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

    override fun onResume() {
        super.onResume()
        model.checkSyncSettings()
    }

    /* UI components */

    @Composable
    @SuppressLint("BatteryLife")
    @OptIn(ExperimentalMaterial3Api::class)
    fun ActivityContent(paddingValues: PaddingValues) {
        val context = LocalContext.current

        val syncing by model.isRefreshing.observeAsState(initial = true)
        val pullRefreshState = rememberPullToRefreshState()
        if (pullRefreshState.isRefreshing) LaunchedEffect(true) {
            pullRefreshState.startRefresh()
            onRefreshRequested()
        }
        if (!syncing) LaunchedEffect(true) {
            delay(1000) // So we can see the spinner shortly, when sync finishes super fast
            pullRefreshState.endRefresh()
        }

        val subscriptions by model.subscriptions.observeAsState()

        val askForCalendarPermission by model.askForCalendarPermission
        val askForNotificationPermission by model.askForNotificationPermission
        val askForWhitelisting by model.askForWhitelisting
        val askForAutoRevoke by model.askForAutoRevoke

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            PullToRefreshContainer(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f),
                state = pullRefreshState,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )

            // progress indicator
            AnimatedVisibility(syncing) {
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

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
                if (askForWhitelisting) {
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
                            val intent = Intent()
                            val pm : PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                intent.action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                            } else {
                                intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                                intent.data = Uri.parse("package:${context.packageName}")
                            }
                            startActivity(intent)
                        }
                    }
                }

                // Auto Revoke permissions warning
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && askForAutoRevoke) {
                    item(key = "auto-revoke-whitelisting") {
                        ActionCard(
                            title = stringResource(R.string.calendar_list_autorevoke_permissions_title),
                            message = stringResource(R.string.calendar_list_autorevoke_permissions_text, stringResource(R.string.app_name)),
                            actionText = stringResource(R.string.calendar_list_battery_whitelist_open_settings),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .animateItemPlacement()
                        ) {
                            Toast.makeText(
                                this@CalendarListActivity,
                                R.string.calendar_list_autorevoke_permissions_instruction,
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(
                                Intent.ACTION_AUTO_REVOKE_PERMISSIONS,
                                Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            ))
                        }
                    }
                }

                if (subscriptions?.isEmpty() == true) {
                    item(key = "empty") {
                        Text(
                            text = stringResource(R.string.calendar_list_empty_info),
                            style = MaterialTheme.typography.bodyLarge,
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
                        val intent = Intent(context, EditCalendarActivity::class.java)
                        intent.putExtra(EditCalendarActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
                        startActivity(intent)
                    })
                }
            }
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
                text = { Text(stringResource(R.string.calendar_list_set_sync_interval)) },
                onClick = {
                    showMenu = false
                    showSyncIntervalDialog = true
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.calendar_list_synchronize)) },
                onClick = {
                    showMenu = false
                    onRefreshRequested()
                }
            )
            DropdownMenuItem(
                text = {
                    val forceDarkMode by settings.forceDarkModeLive().observeAsState(false)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.settings_force_dark_theme))
                        Checkbox(
                            checked = forceDarkMode,
                            onCheckedChange = { onToggleDarkMode() }
                        )
                    }
                },
                onClick =  {
                    showMenu = false
                    onToggleDarkMode()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.calendar_list_privacy_policy)) },
                onClick = {
                    showMenu = false
                    UriUtils.launchUri(context, Uri.parse(PRIVACY_POLICY_URL))
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.calendar_list_info)) },
                onClick = {
                    showMenu = false
                    startActivity(Intent(context, InfoActivity::class.java))
                }
            )
        }
    }


    /* actions */

    private fun onRefreshRequested() = SyncWorker.run(this, true)

    private fun onToggleDarkMode() {
        val settings = Settings(this)
        val newMode = !settings.forceDarkMode()

        settings.forceDarkMode(newMode)
    }


    class SubscriptionsModel(application: Application): AndroidViewModel(application) {

        val askForCalendarPermission = mutableStateOf(false)
        val askForNotificationPermission = mutableStateOf(false)

        val askForWhitelisting = mutableStateOf(false)

        val askForAutoRevoke = mutableStateOf(false)


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
            askForNotificationPermission.value = !haveNotificationPermission

            val haveCalendarPermission = PermissionUtils.haveCalendarPermissions(getApplication())
            askForCalendarPermission.value = !haveCalendarPermission

            val powerManager = getApplication<Application>().getSystemService<PowerManager>()
            val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)

            // If not ignoring battery optimizations, and sync interval is less than a day
            val shouldWhitelistApp = isIgnoringBatteryOptimizations == false
            askForWhitelisting.value = shouldWhitelistApp

            // Make sure permissions are not revoked automatically
            val isAutoRevokeWhitelisted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                getApplication<Application>().packageManager.isAutoRevokeWhitelisted
            } else {
                true
            }
            askForAutoRevoke.value = !isAutoRevokeWhitelisted
        }

    }

}