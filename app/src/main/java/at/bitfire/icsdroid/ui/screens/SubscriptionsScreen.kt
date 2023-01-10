package at.bitfire.icsdroid.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants.ONE_DAY
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.ui.InfoActivity
import at.bitfire.icsdroid.ui.activity.MainActivity.Companion.Paths
import at.bitfire.icsdroid.ui.dialog.SyncIntervalDialog
import at.bitfire.icsdroid.ui.list.SubscriptionListItem
import at.bitfire.icsdroid.ui.model.CalendarModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
fun SubscriptionsScreen(navHostController: NavHostController, model: CalendarModel) {
    val context = LocalContext.current

    var showSyncIntervalDialog by remember { mutableStateOf(false) }

    if (showSyncIntervalDialog)
        SyncIntervalDialog { showSyncIntervalDialog = false }

    val snackbarHostState = remember { SnackbarHostState() }

    val syncInterval by AppAccount.syncInterval.observeAsState()

    // Show an snackbar or dismiss the current one when sync interval is changed
    LaunchedEffect(syncInterval) {
        snapshotFlow { syncInterval }
            .distinctUntilChanged()
            .collect { syncInterval ->
                // TODO: Warnings are only shown the first time you open the app, or when settings are updated
                when {
                    // Show warning if periodic sync not enabled
                    syncInterval == AppAccount.SYNC_INTERVAL_MANUALLY ->
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.calendar_list_sync_interval_manually),
                            duration = SnackbarDuration.Indefinite,
                        )

                    // Show warning if battery optimization is enabled
                    // Battery optimizations ignore was introduced in Android M
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            // If battery optimization is enabled
                            !(context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                                .isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
                            // The sync interval is set to less than a day
                            AppAccount.syncInterval(context) < ONE_DAY &&
                            // And there's no snackbar currently being shown
                            snackbarHostState.currentSnackbarData == null ->
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.calendar_list_battery_whitelist),
                            actionLabel = context.getString(R.string.calendar_list_battery_whitelist_settings),
                            duration = SnackbarDuration.Indefinite,
                        ).also { snackbarResult ->
                            // Check if an action has been performed, ie clicking the settings text
                            if (snackbarResult == SnackbarResult.ActionPerformed)
                                context.startActivity(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                        }

                    // If any alert to be shown, dismiss all
                    else -> snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
    }

    Scaffold(
        topBar = {
            var menuExpanded by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text(stringResource(R.string.title_activity_calendar_list)) },
                actions = {
                    IconButton(onClick = { menuExpanded = !menuExpanded }) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.overflow))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            onClick = { showSyncIntervalDialog = true },
                            text = { Text(stringResource(R.string.calendar_list_set_sync_interval)) },
                        )
                        DropdownMenuItem(
                            onClick = {
                                context.startActivity(Intent(context, InfoActivity::class.java))
                            },
                            text = { Text(stringResource(R.string.calendar_list_info)) },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { Paths.Create.navigate(navHostController) }) {
                Icon(
                    Icons.Rounded.Add,
                    stringResource(R.string.activity_add_calendar),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        val isRefreshing by model.isRefreshing.observeAsState(false)
        val subscriptions by model.subscriptions.observeAsState(emptyList())

        val refreshState = rememberPullRefreshState(isRefreshing, { SyncWorker.run(context, true) })

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .pullRefresh(refreshState),
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(subscriptions) { subscription ->
                    SubscriptionListItem(subscription) {
                        // When an item is selected, navigate to the given subscription
                        Paths.Subscription.navigate(
                            navHostController,
                            "id" to subscription.id,
                        )
                    }
                }
            }

            // Show a message when no subscriptions have been created
            AnimatedVisibility(
                visible = subscriptions.isEmpty(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.calendar_list_empty_info),
                    textAlign = TextAlign.Center,
                )
            }

            // Allow pull-to-refresh
            PullRefreshIndicator(isRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
        }
    }
}
