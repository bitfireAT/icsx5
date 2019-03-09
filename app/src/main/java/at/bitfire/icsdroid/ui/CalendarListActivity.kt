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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.work.WorkInfo
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.db.LocalCalendar
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.calendar_list_activity.*
import kotlinx.android.synthetic.main.calendar_list_item.view.*
import java.text.DateFormat
import java.util.*
import kotlin.concurrent.thread

class CalendarListActivity:
        AppCompatActivity(),
        AdapterView.OnItemClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private var listAdapter: CalendarListAdapter? = null

    private var snackBar: Snackbar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.title_activity_calendar_list)
        setContentView(R.layout.calendar_list_activity)

        refresh.setColorSchemeColors(resources.getColor(R.color.lightblue))
        refresh.setOnRefreshListener(this)
        refresh.setSize(SwipeRefreshLayout.LARGE)

        listAdapter = CalendarListAdapter(this)
        calendar_list.adapter = listAdapter
        calendar_list.onItemClickListener = this
        calendar_list.emptyView = emptyInfo

        // startup fragments
        if (savedInstanceState == null)
            ServiceLoader
                    .load(StartupFragment::class.java)
                    .forEach { it.initialize(this) }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
            getModel()
        else
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 0)

        // check sync settings when sync interval has been edited
        supportFragmentManager.registerFragmentLifecycleCallbacks(object: FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                if (f is SyncIntervalDialogFragment)
                    checkSyncSettings()
            }
        }, false)

        // observe whether a sync is running
        SyncWorker.liveStatus().observe(this, Observer { statuses ->
            val running = statuses.any { it.state == WorkInfo.State.RUNNING } ?: false
            Log.d(Constants.TAG, "Sync running: $running")
            refresh.isRefreshing = running
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED })
            getModel()
        else
            finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_calendar_list, menu)
        return true
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val calendar = parent.getItemAtPosition(position) as LocalCalendar

        val i = Intent(this, EditCalendarActivity::class.java)
        i.data = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendar.id)
        startActivity(i)
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
                snackBar = Snackbar.make(coordinator, R.string.calendar_list_sync_interval_manually, Snackbar.LENGTH_INDEFINITE)
                snackBar?.show()
            }

            // automatic sync not enabled
            !ContentResolver.getMasterSyncAutomatically() -> {
                snackBar = Snackbar.make(coordinator, R.string.calendar_list_master_sync_disabled, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.calendar_list_master_sync_enable) {
                            ContentResolver.setMasterSyncAutomatically(true)
                        }
                snackBar?.show()
            }

            // periodic sync enabled AND Android >= 6 AND not whitelisted from battery saving AND sync interval < 1 day
            Build.VERSION.SDK_INT >= 23 &&
                    !(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
                    AppAccount.syncInterval(this) < 86400 -> {
                snackBar = Snackbar.make(coordinator, R.string.calendar_list_battery_whitelist, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.calendar_list_battery_whitelist_settings) { _ ->
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        }
                snackBar?.show()
            }
        }
    }

    private fun getModel() {
        val model = ViewModelProviders.of(this).get(CalendarModel::class.java)
        model.calendars.observe(this, Observer { calendars ->
            listAdapter?.clear()
            listAdapter?.addAll(calendars)

            if (calendars.isNotEmpty()) {
                // funny: use the calendar colors for the sync status
                val colors = calendars.mapNotNull { it.color }.map { it or 0xff000000.toInt() }
                if (colors.isNotEmpty())
                    refresh?.setColorSchemeColors(*colors.toIntArray())
            }
        })
    }


    /* actions */

    fun onAddCalendar(v: View) {
        startActivity(Intent(this, AddCalendarActivity::class.java))
    }

    override fun onRefresh() {
        SyncWorker.run()
    }

    fun onShowInfo(item: MenuItem) {
        startActivity(Intent(this, InfoActivity::class.java))
    }

    fun onSetSyncInterval(item: MenuItem) {
        SyncIntervalDialogFragment().show(supportFragmentManager, "sync_interval")
    }


    /**
     * Data model for this view. Must only be created when the app has calendar permissions!
     */
    class CalendarModel(
            application: Application
    ): AndroidViewModel(application) {
        val calendars = CalendarLiveData(application)

        fun reload() {
            calendars.loadData()
        }
    }

    class CalendarLiveData(
            val context: Context
    ): LiveData<List<LocalCalendar>>() {
        private val resolver = context.contentResolver

        private val observer = object: ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                loadData()
            }
        }

        override fun onActive() {
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, false, observer)
            loadData()
        }

        override fun onInactive() {
            resolver.unregisterContentObserver(observer)
        }

        fun loadData() {
            thread {
                val provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY)
                if (provider != null)
                    try {
                        postValue(LocalCalendar.findAll(AppAccount.get(context), provider))
                    } catch(e: CalendarStorageException) {
                        Log.e(Constants.TAG, "Couldn't load calendar list", e)
                    } finally {
                        provider.release()
                    }
            }
        }
    }


    private class CalendarListAdapter(
            context: Context
    ): ArrayAdapter<LocalCalendar>(context, R.layout.calendar_list_item) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?:
            LayoutInflater.from(context).inflate(R.layout.calendar_list_item, parent, false)

            val calendar = getItem(position)!!
            v.url.text = calendar.url
            v.title.text = calendar.displayName

            v.sync_status.text =
                    if (!calendar.isSynced)
                        context.getString(R.string.calendar_list_sync_disabled)
                    else {
                        if (calendar.lastSync == 0L)
                            context.getString(R.string.calendar_list_not_synced_yet)
                        else
                            DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT).format(Date(calendar.lastSync))
                    }

            calendar.color?.let { v.color.setColor(it) }

            val errorMessage = calendar.errorMessage
            if (errorMessage == null)
                v.error_message.visibility = View.GONE
            else {
                v.error_message.text = errorMessage
                v.error_message.visibility = View.VISIBLE
            }

            return v
        }
    }

}