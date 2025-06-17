package at.bitfire.icsdroid.model

import android.annotation.SuppressLint
import android.app.Application
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
import androidx.lifecycle.AndroidViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.FileInputStream
import java.io.FileOutputStream

class SubscriptionsModel(application: Application): AndroidViewModel(application) {

    private val settings = Settings(application)

    data class UiState(
        val askForCalendarPermission: Boolean = false,
        val askForNotificationPermission: Boolean = false,
        val askForWhitelisting: Boolean = false,
        val askForAutoRevoke: Boolean = false,
    )

    var uiState by mutableStateOf(UiState())
        private set

    /** whether there are running sync workers */
    val isRefreshing = SyncWorker.statusFlow(application).map { workInfos ->
        workInfos.any { it.state == WorkInfo.State.RUNNING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val subscriptionsDao = AppDatabase.getInstance(application).subscriptionsDao()

    /** LiveData watching the subscriptions */
    val subscriptions = subscriptionsDao
        .getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forceDarkMode = settings.forceDarkModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val syncInterval = AppAccount.getSyncIntervalFlow(application)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppAccount.DEFAULT_SYNC_INTERVAL)

    init {
        // Migrate existing SharedPreferences to DataStore
        migrateSharedPreferencesToDataStore()
        // When initialized, update the ask* fields
        checkSyncSettings()
    }

    private fun migrateSharedPreferencesToDataStore() = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
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
        val haveNotificationPermission = PermissionUtils.haveNotificationPermission(getApplication())
        uiState = uiState.copy(askForNotificationPermission = !haveNotificationPermission)

        val haveCalendarPermission = PermissionUtils.haveCalendarPermissions(getApplication())
        uiState = uiState.copy(askForCalendarPermission = !haveCalendarPermission)

        val powerManager = getApplication<Application>().getSystemService<PowerManager>()
        val isIgnoringBatteryOptimizations = powerManager?.isIgnoringBatteryOptimizations(
            BuildConfig.APPLICATION_ID)

        // If not ignoring battery optimizations, and sync interval is less than a day
        val shouldWhitelistApp = isIgnoringBatteryOptimizations == false
        uiState = uiState.copy(askForWhitelisting = shouldWhitelistApp)

        // Make sure permissions are not revoked automatically
        val isAutoRevokeWhitelisted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getApplication<Application>().packageManager.isAutoRevokeWhitelisted
        } else {
            true
        }
        uiState = uiState.copy(askForAutoRevoke = !isAutoRevokeWhitelisted)
    }

    fun onRefreshRequested() {
        SyncWorker.run(getApplication(), force = true)
    }

    fun onForceRefreshRequested() {
        SyncWorker.run(getApplication(), force =true, forceResync = true)
    }

    fun onToggleDarkMode(forceDarkMode: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val settings = Settings(getApplication())
        settings.forceDarkMode(forceDarkMode)
    }

    fun onSyncIntervalChange(interval: Long) {
        AppAccount.setSyncInterval(getApplication(), interval)
    }

    @SuppressLint("BatteryLife")
    fun onBatteryOptimizationWhitelist() {
        val ctx = getApplication<Application>()
        val intent = Intent()
        val pm : PowerManager = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
            intent.action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        } else {
            intent.action = android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            intent.data = Uri.parse("package:${ctx.packageName}")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }

    @SuppressLint("InlinedApi")
    fun onAutoRevoke() {
        Toast.makeText(
            getApplication(),
            R.string.calendar_list_autorevoke_permissions_instruction,
            Toast.LENGTH_LONG
        ).show()

        val intent = Intent(
            Intent.ACTION_AUTO_REVOKE_PERMISSIONS,
            Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        getApplication<Application>().startActivity(intent)
    }

    fun onBackupExportRequested(uri: Uri) {
        val context: Context = getApplication()
        viewModelScope.launch(Dispatchers.IO) {
            val toast = withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.backup_exporting, Toast.LENGTH_LONG)
                    .also { it.show() }
            }

            val subscriptions = subscriptions.value
            Log.i(TAG, "Exporting ${subscriptions.size} subscriptions...")

            val json = JSONArray().apply {
                for (subscription in subscriptions) {
                    put(subscription.toJSON())
                }
            }
            context.contentResolver.openFileDescriptor(uri, "w")?.use { fd ->
                FileOutputStream(fd.fileDescriptor).bufferedWriter().use { output ->
                    output.write(json.toString())
                }
            }

            withContext(Dispatchers.Main) {
                toast.cancel()
                Toast.makeText(context, R.string.backup_exported, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onBackupImportRequested(uri: Uri) {
        val context: Context = getApplication()
        viewModelScope.launch(Dispatchers.IO) {
            val toast = withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.backup_importing, Toast.LENGTH_LONG)
                    .also { it.show() }
            }

            val jsonString = context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                FileInputStream(fd.fileDescriptor).bufferedReader().use { input ->
                    input.readText()
                }
            }
            val jsonArray = JSONArray(jsonString)
            val newSubscriptions = (0 until jsonArray.length())
                .map { jsonArray.getJSONObject(it) }
                .map { Subscription(it) }
            Log.i(TAG, "Importing ${newSubscriptions.size} subscriptions...")

            val oldSubscriptions = subscriptions.value

            for (subscription in newSubscriptions) {
                val existingSubscription = oldSubscriptions.find { it.url == subscription.url }
                if (existingSubscription != null) {
                    Log.w(TAG, "Overriding existing subscription (${existingSubscription.id}): ${existingSubscription.url}")
                    subscriptionsDao.delete(existingSubscription)
                }
                subscriptionsDao.add(subscription)
            }

            withContext(Dispatchers.Main) {
                toast.cancel()
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(R.plurals.backup_imported, newSubscriptions.size, newSubscriptions.size),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
