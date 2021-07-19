/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
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
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.databinding.CalendarListActivityBinding
import at.bitfire.icsdroid.databinding.CalendarListItemBinding
import at.bitfire.icsdroid.db.LocalCalendar
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.*

class CalendarListActivity: AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    private val model by viewModels<CalendarModel>()
    private lateinit var binding: CalendarListActivityBinding

    private var snackBar: Snackbar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_activity_calendar_list)

        binding = DataBindingUtil.setContentView(this, R.layout.calendar_list_activity)
        binding.lifecycleOwner = this
        binding.model = model

        val defaultRefreshColor = resources.getColor(R.color.lightblue)
        binding.refresh.setColorSchemeColors(defaultRefreshColor)
        binding.refresh.setOnRefreshListener(this)
        binding.refresh.setSize(SwipeRefreshLayout.LARGE)

        model.askForPermissions.observe(this) { ask ->
            if (ask)
                ActivityCompat.requestPermissions(this, CalendarModel.PERMISSIONS, 0)
        }

        model.isRefreshing.observe(this) { isRefreshing ->
            binding.refresh.isRefreshing = isRefreshing
        }

        val calendarAdapter = CalendarListAdapter(application)
        calendarAdapter.clickListener = { calendar ->
            val intent = Intent(this, EditCalendarActivity::class.java)
            intent.data = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendar.id)
            startActivity(intent)
        }
        binding.calendarList.adapter = calendarAdapter

        model.calendars.observe(this) { calendars ->
            calendarAdapter.submitList(calendars)

            val colors = mutableSetOf<Int>()
            colors += defaultRefreshColor
            colors.addAll(calendars.mapNotNull { it.color })
            binding.refresh.setColorSchemeColors(*colors.toIntArray())
        }
        model.reinit()

        // startup fragments
        if (savedInstanceState == null)
            ServiceLoader
                .load(StartupFragment::class.java)
                .forEach { it.initialize(this) }

        // check sync settings when sync interval has been edited
        supportFragmentManager.registerFragmentLifecycleCallbacks(object: FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                if (f is SyncIntervalDialogFragment)
                    checkSyncSettings()
            }
        }, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED })
            model.reinit()
        else {
            Toast.makeText(this, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
            finish()
        }
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

            // automatic sync not enabled
            !ContentResolver.getMasterSyncAutomatically() -> {
                snackBar = Snackbar.make(binding.coordinator, R.string.calendar_list_master_sync_disabled, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.calendar_list_master_sync_enable) {
                            ContentResolver.setMasterSyncAutomatically(true)
                        }.also {
                            it.show()
                        }
            }

            // periodic sync enabled AND Android >= 6 AND not whitelisted from battery saving AND sync interval < 1 day
            Build.VERSION.SDK_INT >= 23 &&
                    !(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
                    AppAccount.syncInterval(this) < 86400 -> {
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

    fun onAddCalendar(v: View) {
        startActivity(Intent(this, AddCalendarActivity::class.java))
    }

    override fun onRefresh() {
        SyncWorker.run(this)
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


    class CalendarListAdapter(
            val context: Context
    ): ListAdapter<LocalCalendar, CalendarListAdapter.ViewHolder>(object: DiffUtil.ItemCallback<LocalCalendar>() {

        override fun areItemsTheSame(oldItem: LocalCalendar, newItem: LocalCalendar) =
                oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: LocalCalendar, newItem: LocalCalendar) =
                // compare all displayed fields
                oldItem.url == newItem.url &&
                oldItem.displayName == newItem.displayName &&
                oldItem.isSynced == newItem.isSynced &&
                oldItem.lastSync == newItem.lastSync &&
                oldItem.color == newItem.color &&
                oldItem.errorMessage == newItem.errorMessage

    }) {

        class ViewHolder(val binding: CalendarListItemBinding): RecyclerView.ViewHolder(binding.root)


        var clickListener: ((LocalCalendar) -> Unit)? = null


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            Log.i(Constants.TAG, "Creating view holder")
            val binding = CalendarListItemBinding.inflate(LayoutInflater.from(context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val calendar = currentList[position]

            holder.binding.root.setOnClickListener {
                clickListener?.let { listener ->
                    listener(calendar)
                }
            }

            holder.binding.apply {
                url.text = calendar.url
                title.text = calendar.displayName

                syncStatus.text =
                    if (!calendar.isSynced)
                        context.getString(R.string.calendar_list_sync_disabled)
                    else {
                        if (calendar.lastSync == 0L)
                            context.getString(R.string.calendar_list_not_synced_yet)
                        else
                            DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT)
                                .format(Date(calendar.lastSync))
                    }

                calendar.color?.let {
                    color.setColor(it)
                }
            }

            val errorMessage = calendar.errorMessage
            if (errorMessage == null)
                holder.binding.errorMessage.visibility = View.GONE
            else {
                holder.binding.errorMessage.text = errorMessage
                holder.binding.errorMessage.visibility = View.VISIBLE
            }
        }

    }


    /**
     * Data model for this view. Must only be created when the app has calendar permissions!
     */
    class CalendarModel(
        application: Application
    ): AndroidViewModel(application) {

        companion object {
            val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        }

        private val resolver = application.contentResolver

        val askForPermissions = MutableLiveData(false)

        /** whether there are running sync workers */
        val isRefreshing = Transformations.map(SyncWorker.liveStatus(application)) { workInfos ->
            workInfos.any { it.state == WorkInfo.State.RUNNING }
        }

        val calendars = MutableLiveData<List<LocalCalendar>>()
        private var observer: ContentObserver? = null


        fun reinit() {
            val havePermissions = PERMISSIONS.all { ActivityCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED }
            askForPermissions.value = !havePermissions

            if (havePermissions && observer == null)
                startWatchingCalendars()
        }

        override fun onCleared() {
            stopWatchingCalendars()
        }


        private fun startWatchingCalendars() {
            val newObserver = object: ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    loadCalendars()
                }
            }
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, false, newObserver)
            observer = newObserver

            loadCalendars()
        }

        private fun stopWatchingCalendars() {
            observer?.let {
                resolver.unregisterContentObserver(it)
                observer = null
            }
        }

        private fun loadCalendars() {
            val provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
            if (provider != null)
                try {
                    val result = LocalCalendar.findAll(AppAccount.get(getApplication()), provider)
                    calendars.postValue(result)
                } catch(e: CalendarStorageException) {
                    Log.e(Constants.TAG, "Couldn't load calendar list", e)
                } finally {
                    provider.release()
                }
        }

    }

}