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
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import kotlinx.android.synthetic.main.edit_calendar.*
import java.net.URI

class EditCalendarActivity: AppCompatActivity(), LoaderManager.LoaderCallbacks<LocalCalendar> {

    companion object {
        private const val STATE_DIRTY = "dirty"
    }

    private var dirty = false      // indicates whether title/color have been changed by the user
        set(value) {
            field = value
            invalidateOptionsMenu()
        }
    private var calendar: LocalCalendar? = null

    private var fragTitleColor: TitleColorFragment? = null
    private var fragCredentials: CredentialsFragment? = null


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)
        setContentView(R.layout.edit_calendar)

        sync_calendar.setOnClickListener { _ ->
            dirty = true
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED)
            // load calendar from provider
            LoaderManager.getInstance(this).initLoader(0, null, this)
        else
            finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_DIRTY, dirty)
    }

    override fun onRestoreInstanceState(inState: Bundle?) {
        super.onRestoreInstanceState(inState)
        inState?.getBoolean(STATE_DIRTY)?.let { dirty = it }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_calendar_activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.delete)
                .setEnabled(!dirty)
                .setVisible(!dirty)

        menu.findItem(R.id.cancel)
                .setEnabled(dirty)
                .setVisible(dirty)

        val titleOK = !fragTitleColor?.title.isNullOrBlank()
        val authOK = fragCredentials?.let {
            !it.requiresAuth || (!it.username.isNullOrEmpty() && !it.password.isNullOrEmpty())
        } ?: false
        menu.findItem(R.id.save)
                .setEnabled(dirty && titleOK && authOK)
                .setVisible(dirty && titleOK && authOK)
        return true
    }


    /* user actions */

    override fun onBackPressed() {
        if (dirty)
            supportFragmentManager.beginTransaction()
                    .add(SaveDismissDialogFragment(), null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
        else
            super.onBackPressed()
    }

    fun onSave(item: MenuItem?) {
        var success = false
        calendar?.let { calendar ->
            try {
                val values = ContentValues(3)
                values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, fragTitleColor?.title)
                values.put(CalendarContract.Calendars.CALENDAR_COLOR, fragTitleColor?.color)
                values.put(CalendarContract.Calendars.SYNC_EVENTS, if (sync_calendar.isChecked) 1 else 0)
                calendar.update(values)

                fragCredentials?.let {
                    if (it.requiresAuth)
                        CalendarCredentials.putCredentials(this, calendar, it.username, it.password)
                    else
                        CalendarCredentials.putCredentials(this, calendar, null, null)
                }

                success = true
            } catch(e: CalendarStorageException) {
                Log.e(Constants.TAG, "Couldn't update calendar", e)
            }
        }
        Toast.makeText(this, getString(if (success) R.string.edit_calendar_saved else R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onAskDelete(item: MenuItem) {
        supportFragmentManager.beginTransaction()
                .add(DeleteDialogFragment(), null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
    }

    private fun onDelete() {
        var success = false
        calendar?.let {
            try {
                it.delete()
                CalendarCredentials.putCredentials(this, it, null, null)
                success = true
            } catch(e: CalendarStorageException) {
                Log.e(Constants.TAG, "Couldn't delete calendar")
            }
        }
        Toast.makeText(this, getString(if (success) R.string.edit_calendar_deleted else R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show()
        finish()
    }

    fun onCancel(item: MenuItem?) {
        finish()
    }

    fun onShare(item: MenuItem) {
        calendar?.let {
            ShareCompat.IntentBuilder.from(this)
                    .setSubject(it.displayName)
                    .setText(it.url)
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
        }
    }


    /* loader callbacks */

    override fun onCreateLoader(id: Int, args: Bundle?) =
            CalendarLoader(this, intent.data!!)

    override fun onLoadFinished(loader: Loader<LocalCalendar>, calendar: LocalCalendar?) {
        if (calendar == null)
            // calendar not available (anymore), close activity
            finish()
        else {
            this.calendar = calendar

            fragTitleColor = supportFragmentManager.findFragmentById(R.id.title_color) as? TitleColorFragment
            if (fragTitleColor == null) {
                val frag = TitleColorFragment()
                val args = Bundle(3)
                args.putString(TitleColorFragment.ARG_URL, calendar.name)
                args.putString(TitleColorFragment.ARG_TITLE, calendar.displayName)
                args.putInt(TitleColorFragment.ARG_COLOR, calendar.color ?: LocalCalendar.DEFAULT_COLOR)
                frag.arguments = args
                frag.setOnChangeListener(object : TitleColorFragment.OnChangeListener {
                    override fun onChangeTitleColor(title: String?, color: Int) {
                        dirty = true
                    }
                })

                supportFragmentManager.beginTransaction()
                        .replace(R.id.title_color, frag)
                        .commit()
                fragTitleColor = frag

                sync_calendar.isChecked = calendar.isSynced
            }

            fragCredentials = supportFragmentManager.findFragmentById(R.id.credentials) as? CredentialsFragment
            if (fragCredentials == null) try {
                val uri = URI(calendar.url)
                if (!uri.scheme.equals("file", true)) {
                    val (username, password) = CalendarCredentials.getCredentials(this, calendar)

                    val frag = CredentialsFragment.newInstance(username, password)
                    frag.setOnChangeListener(object : CredentialsFragment.OnCredentialsChangeListener {
                        override fun onChangeCredentials(username: String?, password: String?) {
                            dirty = true
                        }
                    })

                    val ft = supportFragmentManager.beginTransaction()
                            .replace(R.id.credentials, frag)
                            .commit()
                    fragCredentials = frag
                }
            } catch(e: Exception) {
                Log.e(Constants.TAG, "Invalid calendar URI", e)
            }
        }
    }

    override fun onLoaderReset(loader: Loader<LocalCalendar>) {
        calendar = null
    }


    /* loader */

    class CalendarLoader(
            context: Context,
            private val uri: Uri
    ): Loader<LocalCalendar>(context) {
        companion object {
            const val TAG = "ICSdroid.Calendar"
        }

        private var loaded = false

        private var provider: ContentProviderClient? = null
        private lateinit var observer: ContentObserver

        @SuppressLint("Recycle")
        override fun onStartLoading() {
            val resolver = context.contentResolver
            provider = resolver.acquireContentProviderClient(CalendarContract.AUTHORITY)

            observer = ForceLoadContentObserver()
            resolver.registerContentObserver(uri, false, observer)

            if (!loaded)
                forceLoad()
        }

        override fun onStopLoading() {
            context.contentResolver.unregisterContentObserver(observer)
            provider?.release()
        }

        override fun onForceLoad() {
            var calendar: LocalCalendar? = null
            provider?.let {
                try {
                    calendar = LocalCalendar.findById(AppAccount.account, it, ContentUris.parseId(uri))
                } catch(e: Exception) {
                    Log.e(TAG, "Couldn't load calendar data", e)
                }
            }
            deliverResult(calendar)
        }

    }


    /* "Save or dismiss" dialog */

    class SaveDismissDialogFragment: DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?) =
                AlertDialog.Builder(requireActivity())
                        .setTitle(R.string.edit_calendar_unsaved_changes)
                        .setPositiveButton(R.string.edit_calendar_save) { dialog, _ ->
                            dialog.dismiss()
                            (activity as? EditCalendarActivity)?.onSave(null)
                        }
                        .setNegativeButton(R.string.edit_calendar_dismiss) { dialog, _ ->
                            dialog.dismiss()
                            (activity as? EditCalendarActivity)?.onCancel(null)
                        }
                        .create()!!

    }


    /* "Really delete?" dialog */

    class DeleteDialogFragment: DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?) =
                AlertDialog.Builder(requireActivity())
                        .setMessage(R.string.edit_calendar_really_delete)
                        .setPositiveButton(R.string.edit_calendar_delete) { dialog, _ ->
                            dialog.dismiss()
                            (activity as EditCalendarActivity?)?.onDelete()
                        }
                        .setNegativeButton(R.string.edit_calendar_cancel) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()!!

    }

}
