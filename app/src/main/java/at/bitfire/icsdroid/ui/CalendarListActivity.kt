/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.WorkInfo
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.databinding.CalendarListActivityBinding
import at.bitfire.icsdroid.databinding.CalendarListItemBinding
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.EditCalendarActivity.Companion.EXTRA_SUBSCRIPTION_ID
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.*

class CalendarListActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    companion object {
        /**
         * Represents one day in seconds.
         */
        const val ONE_DAY = 86400
    }

    /**
     * The ViewModel used for doing background work, and watching subscriptions.
     */
    private val model by viewModels<CalendarModel>()

    /**
     * Stores the view binding of the activity.
     */
    private lateinit var binding: CalendarListActivityBinding

    /**
     * Stores the snackbar currently being shown.
     */
    private var snackBar: Snackbar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_activity_calendar_list)

        binding = DataBindingUtil.setContentView(this, R.layout.calendar_list_activity)
        binding.lifecycleOwner = this
        binding.model = model

        val defaultRefreshColor = ContextCompat.getColor(this, R.color.lightblue)
        binding.refresh.setColorSchemeColors(defaultRefreshColor)
        binding.refresh.setOnRefreshListener(this)
        binding.refresh.setSize(SwipeRefreshLayout.LARGE)

        model.requiredPermissions.observe(this) { permissions ->
            if (permissions.isEmpty()) return@observe

            val request = PermissionUtils.registerPermissionRequest(this, permissions, R.string.permissions_required) {
                // we have calendar permissions, cancel possible sync notification (see SyncAdapter.onSecurityException askPermissionsIntent)
                val nm = NotificationManagerCompat.from(this)
                nm.cancel(NotificationUtils.NOTIFY_PERMISSION)
            }
            request()
        }

        // show whether sync is running
        model.isRefreshing.observe(this) { isRefreshing ->
            binding.refresh.isRefreshing = isRefreshing
        }

        // calendars
        val subscriptionsAdapter = SubscriptionListAdapter()
        subscriptionsAdapter.clickListener = { subscription ->
            val intent = Intent(this, EditCalendarActivity::class.java).apply {
                putExtra(EXTRA_SUBSCRIPTION_ID, subscription.id)
            }
            startActivity(intent)
        }
        binding.calendarList.adapter = subscriptionsAdapter

        binding.fab.setOnClickListener {
            onAddCalendar()
        }

        model.subscriptions.observe(this) { subscriptions ->
            subscriptionsAdapter.submitList(subscriptions)

            val colors = subscriptions.mapNotNull { it.color }.toMutableSet()
            colors += defaultRefreshColor
            binding.refresh.setColorSchemeColors(*colors.toIntArray())
        }

        // Check for missing permissions and ask for them
        model.checkPermissions()

        // startup fragments
        if (savedInstanceState == null)
            ServiceLoader
                .load(StartupFragment::class.java)
                .forEach { it.initialize(this) }

        // check sync settings when sync interval has been edited
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                if (f is SyncIntervalDialogFragment)
                    checkSyncSettings()
            }
        }, false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_calendar_list, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.force_dark_mode).isChecked = Settings(this).forceDarkMode()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        checkSyncSettings()
    }


    @SuppressLint("ShowToast")
    private fun checkSyncSettings() {
        snackBar?.dismiss()
        snackBar = null

        when {
            // periodic sync not enabled
            AppAccount.syncInterval(this) == AppAccount.SYNC_INTERVAL_MANUALLY -> {
                snackBar = Snackbar.make(binding.coordinator, R.string.calendar_list_sync_interval_manually, Snackbar.LENGTH_INDEFINITE).also {
                    it.show()
                }
            }

            // periodic sync enabled AND Android >= 6 AND not whitelisted from battery saving AND sync interval < 1 day
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
                    AppAccount.syncInterval(this) < ONE_DAY -> {
                snackBar = Snackbar.make(binding.coordinator, R.string.calendar_list_battery_whitelist, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.calendar_list_battery_whitelist_settings) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                    }.also {
                        it.show()
                    }
            }
        }
    }


    /* actions */

    fun onAddCalendar() {
        startActivity(Intent(this, AddCalendarActivity::class.java))
    }

    override fun onRefresh() {
        SyncWorker.run(this, true)
    }

    fun onShowInfo(item: MenuItem) {
        startActivity(Intent(this, InfoActivity::class.java))
    }

    fun onSetSyncInterval(item: MenuItem) {
        SyncIntervalDialogFragment().show(supportFragmentManager, "sync_interval")
    }

    fun onToggleDarkMode(item: MenuItem) {
        val settings = Settings(this)
        val newMode = !settings.forceDarkMode()
        settings.forceDarkMode(newMode)
        AppCompatDelegate.setDefaultNightMode(
            if (newMode)
                AppCompatDelegate.MODE_NIGHT_YES
            else
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
    }

    /**
     * Provides a list adapter for the subscriptions list.
     */
    inner class SubscriptionListAdapter : ListAdapter<Subscription, SubscriptionListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Subscription>() {
        override fun areItemsTheSame(oldItem: Subscription, newItem: Subscription): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Subscription, newItem: Subscription): Boolean = oldItem == newItem
    }) {
        /**
         * Will get invoked when an item is clicked. Provides the subscription tapped.
         */
        var clickListener: ((subscription: Subscription) -> Unit)? = null

        inner class ViewHolder(val binding: CalendarListItemBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = CalendarListItemBinding.inflate(LayoutInflater.from(this@CalendarListActivity))
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val subscription = currentList[position]

            holder.binding.apply {
                // Invoke click listener when the root is clicked
                root.setOnClickListener { clickListener?.invoke(subscription) }

                // Update the url and title texts
                url.text = subscription.url
                title.text = subscription.displayName

                // Update the sync status text
                syncStatus.text = when {
                    !subscription.isSynced -> getString(R.string.calendar_list_sync_disabled)
                    subscription.lastSync == 0L -> getString(R.string.calendar_list_not_synced_yet)
                    else -> DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT).format(Date(subscription.lastSync))
                }

                // Update the subscription color
                subscription.color?.let { color.setColor(it) }

                // If there's an error message, display, hide the text otherwise
                subscription.errorMessage?.let {
                    errorMessage.text = it
                    errorMessage.visibility = View.VISIBLE
                } ?: run {
                    errorMessage.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Data model for this view. Updates calendar subscriptions in real-time.
     */
    class CalendarModel(
        application: Application
    ) : AndroidViewModel(application) {

        /**
         * A reference to the instance of the Room database.
         */
        private val database = AppDatabase.getInstance(application)

        /**
         * Gets updated with the results of the permissions check. If the array is empty, it means that no permissions shall be asked for. Otherwise, the user
         * must be requested all the permissions given.
         * @see checkPermissions
         */
        val requiredPermissions: MutableLiveData<Array<String>> = MutableLiveData(emptyArray())

        /** whether there are running sync workers */
        val isRefreshing = Transformations.map(SyncWorker.liveStatus(application)) { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING }
        }

        /**
         * Provides a LiveData that gets updated with all the subscriptions made in the database.
         */
        val subscriptions = database.subscriptionsDao().getAllLive()

        /**
         * Checks if all the required permissions are granted. This includes:
         * - Notification permission (API 33+)
         * Updates [requiredPermissions] with the result of the check.
         * @see requiredPermissions
         */
        fun checkPermissions() {
            val permissions = arrayListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)

            requiredPermissions.postValue(permissions.toTypedArray())
        }
    }

}