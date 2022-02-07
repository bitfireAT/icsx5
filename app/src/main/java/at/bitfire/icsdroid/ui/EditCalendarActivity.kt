/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import at.bitfire.ical4android.CalendarStorageException
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.EditCalendarBinding
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val ERROR_MESSAGE = "errorMessage"
        const val THROWABLE = "errorThrowable"
    }

    private val model by viewModels<CalendarModel>()
    private val titleColorModel by viewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by viewModels<CredentialsFragment.CredentialsModel>()

    lateinit var binding: EditCalendarBinding


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        val invalidate = Observer<Any> {
            invalidateOptionsMenu()
        }

        model.calendar.observe(this) { calendar ->
            if (!model.loaded) {
                onCalendarLoaded(calendar)
                model.loaded = true
            }
        }
        model.active.observe(this, invalidate)

        titleColorModel.title.observe(this, invalidate)
        titleColorModel.color.observe(this, invalidate)

        credentialsModel.requiresAuth.observe(this, invalidate)
        credentialsModel.username.observe(this, invalidate)
        credentialsModel.password.observe(this, invalidate)

        binding = DataBindingUtil.setContentView<EditCalendarBinding>(this, R.layout.edit_calendar)
        binding.lifecycleOwner = this
        binding.model = model

        if (inState == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                // permissions OK, load calendar from provider
                val uri = intent.data ?: throw IllegalArgumentException("Intent data must be calendar URI")
                model.loadCalendar(uri)
            } else {
                Toast.makeText(this, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
                finish()
            }

            intent.getStringExtra(ERROR_MESSAGE)?.let { error ->
                AlertFragment.create(error, intent.getSerializableExtra(THROWABLE) as? Throwable)
                        .show(supportFragmentManager, null)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_calendar_activity, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val dirty = dirty()
        menu.findItem(R.id.delete)
                .setEnabled(!dirty)
                .setVisible(!dirty)

        menu.findItem(R.id.cancel)
                .setEnabled(dirty)
                .setVisible(dirty)

        // if local file, hide authentication fragment
        val uri = Uri.parse(model.calendar.value?.url)
        binding.credentials.visibility =
            if (HttpUtils.supportsAuthentication(uri))
                View.VISIBLE
            else
                View.GONE

        val titleOK = !titleColorModel.title.value.isNullOrBlank()
        val authOK = credentialsModel.run {
            if (requiresAuth.value == true)
                !username.value.isNullOrEmpty() && !password.value.isNullOrEmpty()
            else
                true
        }
        menu.findItem(R.id.save)
                .setEnabled(dirty && titleOK && authOK)
                .setVisible(dirty && titleOK && authOK)
        return true
    }

    private fun onCalendarLoaded(calendar: LocalCalendar) {
        titleColorModel.url.value = calendar.url
        calendar.displayName.let {
            titleColorModel.originalTitle = it
            titleColorModel.title.value = it
        }
        calendar.color.let {
            titleColorModel.originalColor = it
            titleColorModel.color.value = it
        }

        model.active.value = calendar.isSynced

        val (username, password) = CalendarCredentials(this).get(calendar)
        val requiresAuth = username != null && password != null
        credentialsModel.originalRequiresAuth = requiresAuth
        credentialsModel.requiresAuth.value = requiresAuth
        credentialsModel.originalUsername = username
        credentialsModel.username.value = username
        credentialsModel.originalPassword = password
        credentialsModel.password.value = password
    }


    /* user actions */

    override fun onBackPressed() {
        if (dirty())
            supportFragmentManager.beginTransaction()
                    .add(SaveDismissDialogFragment(), null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
        else
            super.onBackPressed()
    }

    fun onSave(item: MenuItem?) {
        var success = false
        model.calendar.value?.let { calendar ->
            try {
                val values = ContentValues(3)
                values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, titleColorModel.title.value)
                values.put(CalendarContract.Calendars.CALENDAR_COLOR, titleColorModel.color.value)
                values.put(CalendarContract.Calendars.SYNC_EVENTS, if (model.active.value == true) 1 else 0)
                calendar.update(values)

                credentialsModel.let {
                    val credentials = CalendarCredentials(this)
                    if (it.requiresAuth.value == true)
                        credentials.put(calendar, it.username.value, it.password.value)
                    else
                        credentials.put(calendar, null, null)
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
        model.calendar.value?.let {
            try {
                it.delete()
                CalendarCredentials(this).put(it, null, null)
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
        model.calendar.value?.let {
            ShareCompat.IntentBuilder.from(this)
                    .setSubject(it.displayName)
                    .setText(it.url)
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
        }
    }

    private fun dirty(): Boolean {
        val calendar = model.calendar.value ?: return false
        return  calendar.isSynced != model.active.value ||
                titleColorModel.dirty() ||
                credentialsModel.dirty()
    }


    /* view model and data source */

    class CalendarModel(
            application: Application
    ): AndroidViewModel(application) {

        var loaded = false

        var calendar = MutableLiveData<LocalCalendar>()
        val active = MutableLiveData<Boolean>()

        fun loadCalendar(uri: Uri) {
            @SuppressLint("Recycle")
            val provider = getApplication<Application>().contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY) ?: return
            try {
                calendar.value = LocalCalendar.findById(AppAccount.get(getApplication()), provider, ContentUris.parseId(uri))
            } finally {
                provider.release()
            }
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
                        .create()

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
                        .create()

    }

}