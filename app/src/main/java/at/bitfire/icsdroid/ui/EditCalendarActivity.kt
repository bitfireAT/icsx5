/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.SQLException
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.addCallback
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
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.databinding.EditCalendarBinding
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.get
import at.bitfire.icsdroid.db.dao.pop
import at.bitfire.icsdroid.db.dao.put
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.utils.getSerializableCompat
import at.bitfire.icsdroid.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

@Deprecated("Use Jetpack Compose")
class EditCalendarActivity : AppCompatActivity() {

    companion object {
        const val ERROR_MESSAGE = "errorMessage"
        const val THROWABLE = "errorThrowable"

        const val EXTRA_SUBSCRIPTION_ID = "SubscriptionId"
    }

    private val model by viewModels<SubscriptionModel>()
    private val titleColorModel by viewModels<TitleColorFragment.TitleColorModel>()
    private val credentialsModel by viewModels<CredentialsFragment.CredentialsModel>()

    lateinit var binding: EditCalendarBinding


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        val invalidate = Observer<Any> {
            invalidateOptionsMenu()
        }

        model.subscription.observe(this) { subscription ->
            if (!model.loaded) {
                onSubscriptionLoaded(subscription)
                model.loaded = true
            }
        }
        model.active.observe(this, invalidate)

        titleColorModel.title.observe(this, invalidate)
        titleColorModel.color.observe(this, invalidate)
        titleColorModel.ignoreAlerts.observe(this, invalidate)
        titleColorModel.defaultAlarmMinutes.observe(this, invalidate)

        credentialsModel.requiresAuth.observe(this, invalidate)
        credentialsModel.username.observe(this, invalidate)
        credentialsModel.password.observe(this, invalidate)

        binding = DataBindingUtil.setContentView(this, R.layout.edit_calendar)
        binding.lifecycleOwner = this
        binding.model = model

        if (inState == null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
            ) {
                // permissions OK, load calendar from provider
                val id = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1)
                    .takeIf { it >= 0 } ?: throw IllegalArgumentException("Intent data empty (must be calendar URI)")
                try {
                    model.loadSubscription(id)
                } catch (e: FileNotFoundException) {
                    Toast.makeText(this, R.string.could_not_load_calendar, Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
                finish()
            }

            intent.getStringExtra(ERROR_MESSAGE)?.let { error ->
                AlertFragment.create(error, intent.getSerializableCompat(THROWABLE, Throwable::class))
                    .show(supportFragmentManager, null)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) { onBackPressedHandler() }
        else
            onBackPressedDispatcher.addCallback { onBackPressedHandler() }
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
        val uri = model.subscription.value!!.url
        binding.credentials.visibility =
            if (HttpUtils.supportsAuthentication(uri))
                View.VISIBLE
            else
                View.GONE

        val titleOK = !titleColorModel.title.value.isNullOrBlank()
        val authOK = credentialsModel.run {
            if (requiresAuth.value == true)
                username.value != null && password.value != null
            else
                true
        }
        menu.findItem(R.id.save)
            .setEnabled(dirty && titleOK && authOK)
            .setVisible(dirty && titleOK && authOK)
        return true
    }

    private fun onSubscriptionLoaded(subscription: Subscription) {
        titleColorModel.url.value = subscription.url.toString()
        subscription.displayName.let {
            titleColorModel.originalTitle = it
            titleColorModel.title.value = it
        }
        subscription.color.let {
            titleColorModel.originalColor = it
            titleColorModel.color.value = it
        }
        subscription.ignoreEmbeddedAlerts.let {
            titleColorModel.originalIgnoreAlerts = it
            titleColorModel.ignoreAlerts.postValue(it)
        }
        subscription.defaultAlarmMinutes.let {
            titleColorModel.originalDefaultAlarmMinutes = it
            titleColorModel.defaultAlarmMinutes.postValue(it)
        }

        model.active.value = subscription.isSynced

        runBlocking { CredentialsDao.getInstance(this@EditCalendarActivity).get(subscription) }?.let { cred ->
            val (_, username, password) = cred
            val requiresAuth = username != null && password != null
            credentialsModel.originalRequiresAuth = requiresAuth
            credentialsModel.requiresAuth.value = requiresAuth
            credentialsModel.originalUsername = username
            credentialsModel.username.value = username
            credentialsModel.originalPassword = password
            credentialsModel.password.value = password
        }
    }


    /* user actions */

    private fun onBackPressedHandler() {
        if (dirty())
            supportFragmentManager.beginTransaction()
                .add(SaveDismissDialogFragment(), null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
        else
            finish()
    }

    fun onSave(item: MenuItem?) {
        model.save(titleColorModel, credentialsModel).invokeOnCompletion { err ->
            err?.let { Log.e(Constants.TAG, "Could not save changes.", err) }
            runBlocking(Dispatchers.Main) {
                toast(if (err == null) R.string.edit_calendar_saved else R.string.edit_calendar_failed)
            }

            finish()
        }
    }

    fun onAskDelete(item: MenuItem) {
        supportFragmentManager.beginTransaction()
            .add(DeleteDialogFragment(), null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    private fun onDelete() {
        model.delete().invokeOnCompletion { error ->
            error?.let { Log.e(Constants.TAG, "Could not delete subscription.", it) }
            runBlocking(Dispatchers.Main) {
                toast(if (error == null) R.string.edit_calendar_deleted else R.string.edit_calendar_failed)
            }
            finish()
        }
    }

    fun onCancel(item: MenuItem?) {
        finish()
    }

    fun onShare(item: MenuItem) {
        model.subscription.value?.let {
            ShareCompat.IntentBuilder(this)
                .setSubject(it.displayName)
                .setText(it.url.toString())
                .setType("text/plain")
                .setChooserTitle(R.string.edit_calendar_send_url)
                .startChooser()
        }
    }

    private fun dirty(): Boolean {
        val calendar = model.subscription.value ?: return false
        return calendar.isSynced != model.active.value ||
                titleColorModel.dirty() ||
                credentialsModel.dirty()
    }


    /* view model and data source */
    class SubscriptionModel(
        application: Application
    ) : AndroidViewModel(application) {
        var loaded = false

        var subscription = MutableLiveData<Subscription>()
        val active = MutableLiveData<Boolean>()

        private val subscriptionsDao = AppDatabase.getInstance(getApplication()).subscriptionsDao()

        /**
         * Loads the requested calendar from the database into [subscription].
         * @param id  The id of the calendar to look for.
         * @throws FileNotFoundException when the calendar doesn't exist (anymore)
         */
        fun loadSubscription(id: Long) = viewModelScope.launch(Dispatchers.IO) {
            val subscription = subscriptionsDao.getById(id)
            this@SubscriptionModel.subscription.postValue(subscription)
        }

        /**
         * Saves the currently loaded subscription with the data of the given view models. Job may throw exceptions. Runs [SyncWorker].
         * @author Arnau Mora
         * @param titleColorModel The view model containing all the changes made.
         * @param credentialsModel The view model for storing the subscription's credentials.
         * @return A [Job] that keeps track of the task's progress.
         * @throws IllegalStateException If there isn't any loaded subscription ([subscription] is null).
         * @throws SQLException If there's any issue while updating the database.
         */
        fun save(
            titleColorModel: TitleColorFragment.TitleColorModel,
            credentialsModel: CredentialsFragment.CredentialsModel,
        ): Job = viewModelScope.launch(Dispatchers.IO) {
            val subscription = subscription.value ?: throw IllegalStateException("There's no loaded subscription to save.")
            subscriptionsDao.update(
                subscription.copy(
                    displayName = titleColorModel.title.value!!,
                    color = titleColorModel.color.value,
                    syncEvents = active.value ?: false,
                    defaultAlarmMinutes = titleColorModel.defaultAlarmMinutes.value,
                    ignoreEmbeddedAlerts = titleColorModel.ignoreAlerts.value ?: false,
                )
            )

            SyncWorker.run(getApplication(), forceResync = true)

            credentialsModel.let { model ->
                val credentials = CredentialsDao.getInstance(getApplication())
                if (model.requiresAuth.value == true)
                    Credential(subscription, model.username, model.password).let {
                        credentials.put(it)
                    }
                else
                    credentials.pop(subscription)
            }
        }

        /**
         * Deletes the currently loaded subscription. Job may throw exceptions
         * @author Arnau Mora
         * @return A [Job] that keeps track of the task's progress.
         * @throws IllegalStateException If there isn't any loaded subscription ([subscription] is null).
         * @throws SQLException If there's any issue while updating the database.
         */
        fun delete(): Job = viewModelScope.launch(Dispatchers.IO) {
            val subscription = subscription.value ?: throw IllegalStateException("There's no loaded subscription to delete.")
            subscription.delete(getApplication())
            CredentialsDao.getInstance(getApplication()).pop(subscription)
        }
    }


    /* "Save or dismiss" dialog */

    class SaveDismissDialogFragment : DialogFragment() {

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

    class DeleteDialogFragment : DialogFragment() {

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