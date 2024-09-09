package at.bitfire.icsdroid.ui.screen

import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.UriUtils
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.model.SubscriptionsModel
import at.bitfire.icsdroid.ui.partials.ActionCard
import at.bitfire.icsdroid.ui.partials.CalendarListItem
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.SyncIntervalDialog
import at.bitfire.icsdroid.ui.views.CalendarListActivity

@Composable
fun CalendarListScreen(
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

    CalendarListScreen(
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
        onItemSelected = onItemSelected
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CalendarListScreen(
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
                        forceDarkMode = forceDarkMode,
                        syncInterval = syncInterval,
                        onSyncIntervalChange = onSyncIntervalChange,
                        onToggleDarkMode = onToggleDarkMode,
                        onAboutRequested = onAboutRequested,
                        onRefreshRequested = onForceRefreshRequested
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
    forceDarkMode: Boolean,
    syncInterval: Long,
    onSyncIntervalChange: (Long) -> Unit = {},
    onToggleDarkMode: (forceDarkMode: Boolean) -> Unit = {},
    onAboutRequested: () -> Unit = {},
    onRefreshRequested: () -> Unit = {}
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
            text = { Text(stringResource(R.string.calendar_list_privacy_policy)) },
            onClick = {
                showMenu = false
                UriUtils.launchUri(context, Uri.parse(CalendarListActivity.PRIVACY_POLICY_URL))
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
