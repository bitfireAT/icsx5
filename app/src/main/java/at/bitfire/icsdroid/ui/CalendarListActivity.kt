/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.CalendarContract
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.LoaderManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.Loader
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.BuildConfig
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.LocalCalendar
import kotlinx.android.synthetic.main.calendar_list_activity.*
import kotlinx.android.synthetic.main.calendar_list_item.view.*
import java.text.DateFormat
import java.util.*

class CalendarListActivity: AppCompatActivity(), LoaderManager.LoaderCallbacks<List<LocalCalendar>>, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, SyncStatusObserver {

    private var syncStatusHandle: Any? = null
    private var syncStatusHandler: Handler? = null

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

        if (getPreferences(0).getLong(DonateDialogFragment.PREF_NEXT_REMINDER, 0) < System.currentTimeMillis()) {
            val installer = packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID)
            if (installer == null || installer.startsWith("org.fdroid"))
                DonateDialogFragment().show(supportFragmentManager, "donate")
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
            supportLoaderManager.initLoader(0, null, this)
        else
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 0)

        supportFragmentManager.registerFragmentLifecycleCallbacks(object: FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentDestroyed(fm: FragmentManager?, f: Fragment?) {
                if (f is SyncIntervalDialogFragment)
                    checkSyncSettings()
            }
        }, false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.all { it == PackageManager.PERMISSION_GRANTED })
            supportLoaderManager.initLoader(0, null, this)
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
        val handler = Handler(Handler.Callback {
            val syncActive = AppAccount.isSyncActive()
            Log.d(Constants.TAG, "Is sync. active? ${if (syncActive) "yes" else "no"}")
            // workaround: see https://code.google.com/p/android/issues/detail?id=77712
            refresh.post({
                refresh.isRefreshing = syncActive
            })
            true
        })
        syncStatusHandle = ContentResolver.addStatusChangeListener(ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE, this)
        handler.sendEmptyMessage(0)
        syncStatusHandler = handler

        checkSyncSettings()
    }

    override fun onPause() {
        super.onPause()
        if (syncStatusHandle != null)
            ContentResolver.removeStatusChangeListener(syncStatusHandle)
    }

    override fun onStatusChanged(which: Int) {
        if (which == ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE)
            syncStatusHandler?.sendEmptyMessage(0)
    }

    private fun checkSyncSettings() {
        snackBar?.dismiss()
        snackBar = null

        when {
            AppAccount.getSyncInterval(this) == AppAccount.SYNC_INTERVAL_MANUALLY -> {
                snackBar = Snackbar.make(coordinator, R.string.calendar_list_sync_interval_manually, Snackbar.LENGTH_INDEFINITE)
                snackBar?.show()
            }

            !ContentResolver.getMasterSyncAutomatically() -> {
                snackBar = Snackbar.make(coordinator, R.string.calendar_list_master_sync_disabled, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.calendar_list_master_sync_enable, {
                            ContentResolver.setMasterSyncAutomatically(true)
                        })
                snackBar?.show()
            }

            // Android >= 6 AND not whitelisted from battery saving AND sync interval < 1 day
            Build.VERSION.SDK_INT >= 23 &&
                    !(getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID) &&
                    AppAccount.getSyncInterval(this) < 86400 -> {
                snackBar = Snackbar.make(coordinator, R.string.calendar_list_battery_whitelist, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.calendar_list_battery_whitelist_settings, { _ ->
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            startActivity(intent)
                        })
                snackBar?.show()
            }
        }
    }


    /* loader callbacks */

    override fun onCreateLoader(id: Int, args: Bundle?) =
            CalendarListLoader(this)

    override fun onLoadFinished(loader: Loader<List<LocalCalendar>>, calendars: List<LocalCalendar>?) {
        // we got our list of calendars

        // add them into the list
        listAdapter?.clear()

        calendars?.let {
            listAdapter?.addAll(calendars)

            // control the swipe refresher
            if (calendars.isNotEmpty()) {
                // funny: use the calendar colors for the sync status
                val colors = LinkedList<Int>()
                calendars.forEach { calendar ->
                    calendar.color?.let { colors += 0xff000000.toInt() or it }
                }
                if (colors.isNotEmpty())
                    refresh?.setColorSchemeColors(*colors.toIntArray())
            }
        }
    }

    override fun onLoaderReset(loader: Loader<List<LocalCalendar>>) {
    }


    /* actions */

    fun onAddCalendar(v: View) {
        startActivity(Intent(this, AddCalendarActivity::class.java))
    }

    override fun onRefresh() {
        val extras = Bundle(2)
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        ContentResolver.requestSync(AppAccount.account, CalendarContract.AUTHORITY, extras)
    }

    fun onShowInfo(item: MenuItem) {
        startActivity(Intent(this, InfoActivity::class.java))
    }

    fun onSetSyncInterval(item: MenuItem) {
        SyncIntervalDialogFragment().show(supportFragmentManager, "sync_interval")
    }


    /* list adapter */

    private class CalendarListAdapter(
            context: Context
    ): ArrayAdapter<LocalCalendar>(context, R.layout.calendar_list_item) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val v = convertView ?:
                    LayoutInflater.from(context).inflate(R.layout.calendar_list_item, parent, false)

            val calendar = getItem(position)
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

    class CalendarListLoader(
            context: Context
    ): Loader<List<LocalCalendar>>(context) {
        private var provider: ContentProviderClient? = null
        private lateinit var observer: ContentObserver

        @SuppressLint("Recycle")
        override fun onStartLoading() {
            val resolver = context.contentResolver
            provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY)

            observer = ForceLoadContentObserver()
            resolver.registerContentObserver(CalendarContract.Calendars.CONTENT_URI, false, observer)

            forceLoad()
        }

        override fun onStopLoading() {
            context.contentResolver.unregisterContentObserver(observer)
            provider?.release()
        }

        override fun onForceLoad() {
            try {
                var calendars: List<LocalCalendar>? = null
                provider?.let {
                    calendars = LocalCalendar.findAll(AppAccount.account, it)
                }
                deliverResult(calendars)
            } catch(e: CalendarStorageException) {
                Log.e(Constants.TAG, "Couldn't load calendar list", e)
            }
        }

    }

}
