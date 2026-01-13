/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.model

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.PermissionUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.Settings
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.dataStore
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SubscriptionsModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
): ViewModel() {

    private val settings = Settings(context)

    data class UiState(
        val askForCalendarPermission: Boolean = false,
        val askForNotificationPermission: Boolean = false,
        val askForWhitelisting: Boolean = false,
        val askForAutoRevoke: Boolean = false,
    )

    var uiState by mutableStateOf(UiState())
        private set

    /** whether there are running sync workers */
    val isRefreshing = SyncWorker.statusFlow(context).map { workInfos ->
        workInfos.any { it.state == WorkInfo.State.RUNNING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /** LiveData watching the subscriptions */
    val subscriptions = db
        .subscriptionsDao()
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forceDarkMode = settings.forceDarkModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncInterval = AppAccount.getSyncIntervalFlow(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppAccount.DEFAULT_SYNC_INTERVAL)

    init {
        // Migrate existing SharedPreferences to DataStore
        migrateSharedPreferencesToDataStore()
        // When initialized, update the ask* fields
        checkSyncSettings()
    }

    private fun migrateSharedPreferencesToDataStore() = viewModelScope.launch(Dispatchers.IO) {
        val context = context
        val dataStore = context.dataStore
        // These preferences are the ones used in Settings. DonateDialogService in OSE use another
        // storage. Since it's only used for showing a dialog, we don't migrate it for simplicity.
        val prefs: SharedPreferences = context.getSharedPreferences("icsx5", 0)
        @Suppress("DEPRECATION")
        dataStore.edit {
            if (prefs.contains(Settings.FORCE_DARK_MODE)) {
                it[Settings.forceDarkMode] = prefs.getBoolean(Settings.FORCE_DARK_MODE, false)
            }
        }
        // Clear everything
        prefs.edit().clear().apply()
    }

    /**
     * Performs all the checks necessary, and updates [UiState.askForCalendarPermission],
     * [UiState.askForNotificationPermission] and [UiState.askForWhitelisting] which should be shown to the
     * user through a Snackbar.
     */
    fun checkSyncSettings() = viewModelScope.launch(Dispatchers.IO) {
        val haveNotificationPermission = PermissionUtils.haveNotificationPermission(context)

        val haveCalendarPermission = PermissionUtils.haveCalendarPermissions(context)

        val powerManager = context.getSystemService<PowerManager>()
        val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)

        // If not ignoring battery optimizations, and sync interval is less than a day
        val shouldWhitelistApp = isIgnoringBatteryOptimizations == false

        // Make sure permissions are not revoked automatically
        val isAutoRevokeWhitelisted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.isAutoRevokeWhitelisted
        } else {
            true
        }
        uiState = uiState.copy(
            askForAutoRevoke = !isAutoRevokeWhitelisted,
            askForCalendarPermission = !haveCalendarPermission,
            askForNotificationPermission = !haveNotificationPermission,
            askForWhitelisting = shouldWhitelistApp,
        )
    }

    fun onRefreshRequested() {
        SyncWorker.run(context, force = true)
    }

    fun onForceRefreshRequested() {
        SyncWorker.run(context, force =true, forceResync = true)
    }

    fun onToggleDarkMode(forceDarkMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val settings = Settings(context)
        settings.forceDarkMode(forceDarkMode)
    }

    fun onSyncIntervalChange(interval: Long) {
        AppAccount.setSyncInterval(context, interval)
    }

    @SuppressLint("BatteryLife")
    fun onBatteryOptimizationWhitelist() {
        val intent = Intent()
        val pm : PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(context.packageName)) {
            intent.action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        } else {
            intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:${context.packageName}")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    @SuppressLint("InlinedApi")
    fun onAutoRevoke() {
        Toast.makeText(
            context,
            R.string.calendar_list_autorevoke_permissions_instruction,
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(
            Intent.ACTION_AUTO_REVOKE_PERMISSIONS,
            Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        context.startActivity(intent)
    }

    fun onBackupExportRequested(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val toast = withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.backup_exporting, Toast.LENGTH_LONG).apply { show() }
            }

            val subscriptions = subscriptions.value
            Log.i(TAG, "Exporting ${subscriptions.size} subscriptions...")

            val json = JSONArray().apply {
                for (subscription in subscriptions) {
                    put(subscription.toJSON())
                }
            }
            try {
                context.contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                    FileOutputStream(fd.fileDescriptor).bufferedWriter().use { output ->
                        output.write(json.toString())
                    }
                }

                withContext(Dispatchers.Main) {
                    toast.cancel()
                    Toast.makeText(context, R.string.backup_exported, Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Could not write export file.", e)
                withContext(Dispatchers.Main) {
                    toast.cancel()
                    Toast.makeText(context, R.string.backup_export_error_io, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun onBackupImportRequested(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val toast = withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.backup_importing, Toast.LENGTH_LONG)
                    .apply { show() }
            }

            try {
                val jsonString = context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                    FileInputStream(fd.fileDescriptor).bufferedReader().use { input ->
                        input.readText()
                    }
                }
                if (jsonString == null) {
                    withContext(Dispatchers.Main) {
                        toast.cancel()
                        Toast.makeText(context, R.string.backup_import_error_io, Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val jsonArray = JSONArray(jsonString)
                val newSubscriptions = (0 until jsonArray.length())
                    .map { jsonArray.getJSONObject(it) }
                    .map { Subscription(it) }
                Log.i(TAG, "Importing ${newSubscriptions.size} subscriptions...")

                val oldSubscriptions = subscriptions.value

                val toAdd = mutableListOf<Subscription>()
                var toDelete = arrayOf<Subscription>()
                for (subscription in newSubscriptions) {
                    val existingSubscription = oldSubscriptions.find { it.url == subscription.url }
                    if (existingSubscription != null) {
                        Log.w(TAG, "Overriding existing subscription (${existingSubscription.id}): ${existingSubscription.url}")
                        toDelete += existingSubscription
                    }
                    toAdd += subscription
                }

                // Run the database updates
                db.subscriptionsDao().delete(*toDelete)
                db.subscriptionsDao().add(toAdd)

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(context)

                withContext(Dispatchers.Main) {
                    toast.cancel()
                    Toast.makeText(
                        context,
                        context.resources.getQuantityString(R.plurals.backup_imported, newSubscriptions.size, newSubscriptions.size),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: JSONException) {
                Log.e(TAG, "Could not load JSON: $e")
                withContext(Dispatchers.Main) {
                    toast.cancel()
                    Toast.makeText(
                        context,
                        R.string.backup_import_error_json,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Could not load JSON: $e")
                withContext(Dispatchers.Main) {
                    toast.cancel()
                    Toast.makeText(
                        context,
                        R.string.backup_import_error_security,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IOException) {
                Log.e(TAG, "Could not load JSON: $e")
                withContext(Dispatchers.Main) {
                    toast.cancel()
                    Toast.makeText(
                        context,
                        R.string.backup_import_error_io,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
