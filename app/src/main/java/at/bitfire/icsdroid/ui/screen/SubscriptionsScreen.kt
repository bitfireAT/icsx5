/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.ui.screen

import android.content.Intent
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import at.bitfire.icsdroid.MainActivity
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.UriUtils
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.model.SubscriptionsModel
import at.bitfire.icsdroid.ui.InfoActivity
import at.bitfire.icsdroid.ui.partials.ActionCard
import at.bitfire.icsdroid.ui.partials.CalendarListItem
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import at.bitfire.icsdroid.ui.partials.SyncIntervalDialog
import at.bitfire.icsdroid.ui.views.EditSubscriptionActivity

@Composable
fun SubscriptionsScreen(
    requestPermissions: Boolean,
    onAddRequested: () -> Unit,
    model: SubscriptionsModel = hiltViewModel()
) {
    val activity = LocalActivity.current
    val context = LocalContext.current

    val requestCalendarPermissions = PermissionUtils.rememberCalendarPermissionRequest {
        model.checkSyncSettings()

        SyncWorker.run(context)
    }
    val requestNotificationPermission = PermissionUtils.rememberNotificationPermissionRequest {
        model.checkSyncSettings()
    }

    LaunchedEffect(Unit) {
        if (requestPermissions && !PermissionUtils.haveCalendarPermissions(context))
            requestCalendarPermissions()

        model.checkSyncSettings()
    }

    SubscriptionsScreen(
        model = model,
        onAboutRequested = {
            activity?.startActivity(Intent(context, InfoActivity::class.java))
        },
        onAddRequested = onAddRequested,
        onRequestCalendarPermissions = requestCalendarPermissions,
        onRequestNotificationPermission = requestNotificationPermission,
        onItemSelected = { subscription ->
            activity?.startActivity(
                Intent(context, EditSubscriptionActivity::class.java)
                    .putExtra(EditSubscriptionActivity.EXTRA_SUBSCRIPTION_ID, subscription.id)
            )
        }
    )
}

@Composable
fun SubscriptionsScreen(
    model: SubscriptionsModel,
    onAboutRequested: () -> Unit,
    onAddRequested: () -> Unit,
    onRequestCalendarPermissions: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onItemSelected: (Subscription) -> Unit
) {
    val uiState = model.uiState
    val subscriptions by model.subscriptions.collectAsState()
    val isRefreshing by model.isRefreshing.collectAsState()
    val forceDarkMode by model.forceDarkMode.collectAsState()
    val syncInterval by model.syncInterval.collectAsState()

    val createFileResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { result ->
        result ?: return@rememberLauncherForActivityResult
        model.onBackupExportRequested(result)
    }
    val loadFileResultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { result ->
        result ?: return@rememberLauncherForActivityResult
        model.onBackupImportRequested(result)
    }

    SubscriptionsScreen(
        isRefreshing = isRefreshing,
        subscriptions = subscriptions,
        uiState = uiState,
        forceDarkMode = forceDarkMode,
        syncInterval = syncInterval,
        onRefreshRequested = model::onRefreshRequested,
        onForceRefreshRequested = model::onForceRefreshRequested,
        onAddRequested = onAddRequested,
        onRequestCalendarPermissions = onRequestCalendarPermissions,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onBatteryOptimizationWhitelist = model::onBatteryOptimizationWhitelist,
        onAutoRevokePermission = model::onAutoRevoke,
        onSyncIntervalChange = model::onSyncIntervalChange,
        onAboutRequested = onAboutRequested,
        onToggleDarkMode = model::onToggleDarkMode,
        onBackupExportRequested = { createFileResultLauncher.launch("icsx5-backup.json") },
        onBackupImportRequested = { loadFileResultLauncher.launch(arrayOf("application/json")) },
        onItemSelected = onItemSelected
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SubscriptionsScreen(
    isRefreshing: Boolean,
    subscriptions: List<Subscription>,
    uiState: SubscriptionsModel.UiState,
    forceDarkMode: Boolean,
    syncInterval: Long,
    onRefreshRequested: () -> Unit = {},
    onForceRefreshRequested: () -> Unit = {},
    onAddRequested: () -> Unit = {},
    onRequestCalendarPermissions: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onBatteryOptimizationWhitelist: () -> Unit = {},
    onAutoRevokePermission: () -> Unit = {},
    onSyncIntervalChange: (Long) -> Unit = {},
    onToggleDarkMode: (forceDarkMode: Boolean) -> Unit = {},
    onAboutRequested: () -> Unit = {},
    onBackupExportRequested: () -> Unit = {},
    onBackupImportRequested: () -> Unit = {},
    onItemSelected: (Subscription) -> Unit = {}
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRequested
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
                    ActionOverflowMenu(
                        subscriptionsCount = subscriptions.size,
                        forceDarkMode = forceDarkMode,
                        syncInterval = syncInterval,
                        onSyncIntervalChange = onSyncIntervalChange,
                        onToggleDarkMode = onToggleDarkMode,
                        onAboutRequested = onAboutRequested,
                        onRefreshRequested = onForceRefreshRequested,
                        onBackupExportRequested = onBackupExportRequested,
                        onBackupImportRequested = onBackupImportRequested
                    )
                }
            )
        }
    ) { paddingValues ->
        CalendarListContent(
            paddingValues,
            isRefreshing,
            subscriptions,
            uiState,
            onRefreshRequested,
            onRequestCalendarPermissions,
            onRequestNotificationPermission,
            onBatteryOptimizationWhitelist,
            onAutoRevokePermission,
            onItemSelected
        )
    }
}

@Composable
@OptIn(
    ExperimentalMaterial3Api::class
)
private fun CalendarListContent(
    paddingValues: PaddingValues,
    isRefreshing: Boolean,
    subscriptions: List<Subscription>,
    uiState: SubscriptionsModel.UiState,
    onRefreshRequested: () -> Unit = {},
    onRequestCalendarPermissions: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onBatteryOptimizationWhitelist: () -> Unit = {},
    onAutoRevokePermission: () -> Unit = {},
    onItemSelected: (Subscription) -> Unit = {}
) {
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = Modifier
            .padding(paddingValues),
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefreshRequested
    ) {
        // progress indicator
        AnimatedVisibility(isRefreshing) {
            LinearProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            // Calendar permission card
            if (uiState.askForCalendarPermission) {
                item(key = "calendar-perm") {
                    ActionCard(
                        title = stringResource(R.string.calendar_permissions_required),
                        message = stringResource(R.string.calendar_permissions_required_text),
                        actionText = stringResource(R.string.permissions_grant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .animateItem(),
                        onAction = onRequestCalendarPermissions
                    )
                }
            }

            // Notification permission card
            if (uiState.askForNotificationPermission) {
                item(key = "notification-perm") {
                    ActionCard(
                        title = stringResource(R.string.notification_permissions_required),
                        message = stringResource(R.string.notification_permissions_required_text),
                        actionText = stringResource(R.string.permissions_grant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .animateItem(),
                        onAction = onRequestNotificationPermission
                    )
                }
            }

            // Whitelisting card
            if (uiState.askForWhitelisting) {
                item(key = "battery-whitelisting") {
                    ActionCard(
                        title = stringResource(R.string.calendar_list_battery_whitelist_title),
                        message = stringResource(R.string.calendar_list_battery_whitelist_text, stringResource(R.string.app_name)),
                        actionText = stringResource(R.string.calendar_list_battery_whitelist_open_settings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .animateItem(),
                        onAction = onBatteryOptimizationWhitelist
                    )
                }
            }

            // Auto Revoke permissions warning
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && uiState.askForAutoRevoke) {
                item(key = "auto-revoke-whitelisting") {
                    ActionCard(
                        title = stringResource(R.string.calendar_list_autorevoke_permissions_title),
                        message = stringResource(R.string.calendar_list_autorevoke_permissions_text, stringResource(R.string.app_name)),
                        actionText = stringResource(R.string.calendar_list_battery_whitelist_open_settings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .animateItem(),
                        onAction = onAutoRevokePermission
                    )
                }
            }

            if (subscriptions.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.calendar_list_empty_info),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp, 16.dp, 8.dp, 8.dp)
                            .animateItem()
                    )
                }
            }

            items(subscriptions) { subscription ->
                CalendarListItem(subscription) { onItemSelected(subscription) }
            }
        }
    }
}

@Composable
fun ActionOverflowMenu(
    subscriptionsCount: Int,
    forceDarkMode: Boolean,
    syncInterval: Long,
    onSyncIntervalChange: (Long) -> Unit = {},
    onToggleDarkMode: (forceDarkMode: Boolean) -> Unit = {},
    onAboutRequested: () -> Unit = {},
    onRefreshRequested: () -> Unit = {},
    onBackupExportRequested: () -> Unit = {},
    onBackupImportRequested: () -> Unit = {}
) {
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }

    IconButton(onClick = { showMenu = true }) {
        Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more))
    }

    var showSyncIntervalDialog by rememberSaveable { mutableStateOf(false) }
    if (showSyncIntervalDialog)
        SyncIntervalDialog(
            currentInterval = syncInterval,
            onSetSyncInterval = onSyncIntervalChange,
            onDismiss = { showSyncIntervalDialog = false }
        )

    var showImportWarningDialog by rememberSaveable { mutableStateOf(false) }
    if (showImportWarningDialog)
        GenericAlertDialog(
            title = stringResource(R.string.backup_warning_title),
            confirmButton = stringResource(android.R.string.ok) to {
                showImportWarningDialog = false
                onBackupImportRequested()
            },
            dismissButton = stringResource(android.R.string.cancel) to {
                showImportWarningDialog = false
                                                                       },
            onDismissRequest = { showImportWarningDialog = false },
            content = { Text(stringResource(R.string.backup_warning_message)) }
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
            text = { Text(stringResource(R.string.calendar_list_force_sync)) },
            onClick = {
                showMenu = false
                onRefreshRequested()
            }
        )
        DropdownMenuItem(
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_force_dark_theme))
                    Checkbox(
                        checked = forceDarkMode,
                        onCheckedChange = { onToggleDarkMode(!forceDarkMode) }
                    )
                }
            },
            onClick =  {
                showMenu = false
                onToggleDarkMode(!forceDarkMode)
            }
        )
        DropdownMenuItem(
            enabled = subscriptionsCount > 0,
            text = { Text(stringResource(R.string.calendar_list_backup_export)) },
            onClick = {
                showMenu = false
                onBackupExportRequested()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.calendar_list_backup_import)) },
            onClick = {
                showMenu = false
                // If there's already a subscription, show a warning before running import
                if (subscriptionsCount > 0)
                    showImportWarningDialog = true
                else
                    onBackupImportRequested()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.calendar_list_privacy_policy)) },
            onClick = {
                showMenu = false
                UriUtils.launchUri(context, MainActivity.PRIVACY_POLICY_URL.toUri())
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.calendar_list_info)) },
            onClick = {
                showMenu = false
                onAboutRequested()
            }
        )
    }
}
