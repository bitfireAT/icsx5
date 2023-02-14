/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.Manifest
import android.app.Application
import android.content.ContentUris
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
import at.bitfire.icsdroid.*
import at.bitfire.icsdroid.databinding.EditCalendarBinding
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val ERROR_MESSAGE = "errorMessage"
        const val THROWABLE = "errorThrowable"
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

        model.subscriptionData.observe(this) { data ->
            if (data == null) return@observe
            val (subscription, credential) = data
            if (!model.loaded) {
                onSubscriptionLoaded(subscription, credential)
                model.loaded = true
            }
        }

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
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                // permissions OK, load calendar from provider
                val uri = intent.data ?: throw IllegalArgumentException("Intent data empty (must be calendar URI)")
                val subscriptionId = ContentUris.parseId(uri)
                try {
                    model.loadSubscription(subscriptionId)
                } catch (e: FileNotFoundException) {
                    Toast.makeText(this, R.string.could_not_load_calendar, Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Toast.makeText(this, R.string.calendar_permissions_required, Toast.LENGTH_LONG).show()
                finish()
            }

            intent.getStringExtra(ERROR_MESSAGE)?.let { error ->
                AlertFragment.create(error, intent.getSerializableExtra(THROWABLE) as? Throwable)
                        .show(supportFragmentManager, null)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) { handleOnBackPressed() }
        else
            onBackPressedDispatcher.addCallback { handleOnBackPressed() }
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
        val uri = model.subscriptionData.value?.first?.url
        binding.credentials.visibility =
            if (uri != null && HttpUtils.supportsAuthentication(uri))
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

    private fun onSubscriptionLoaded(subscription: Subscription, credential: Credential?) {
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

        (credential != null).let { requiresAuth ->
            credentialsModel.originalRequiresAuth = requiresAuth
            credentialsModel.requiresAuth.value = requiresAuth
        }
        credential?.username.let { username ->
            credentialsModel.originalUsername = username ?: ""
            credentialsModel.username.value = username ?: ""
        }
        credential?.password.let { password ->
            credentialsModel.originalPassword = password ?: ""
            credentialsModel.password.value = password ?: ""
        }
    }


    /* user actions */

    private fun handleOnBackPressed() {
        if (dirty())
            supportFragmentManager.beginTransaction()
                    .add(SaveDismissDialogFragment(), null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
    }

    fun onSave(item: MenuItem?) {
        model.updateSubscription(titleColorModel, credentialsModel).invokeOnCompletion { error ->
            if (error == null)
                Toast.makeText(this, getString(R.string.edit_calendar_saved), Toast.LENGTH_SHORT).show()
            else {
                Log.e(Constants.TAG, "Couldn't update calendar", error)
                Toast.makeText(this, getString(R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show()
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
        model.deleteSubscription().invokeOnCompletion { error ->
            if (error == null)
                Toast.makeText(this, getString(R.string.edit_calendar_deleted), Toast.LENGTH_SHORT).show()
            else {
                Log.e(Constants.TAG, "Couldn't update calendar", error)
                Toast.makeText(this, getString(R.string.edit_calendar_failed), Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    fun onCancel(item: MenuItem?) {
        finish()
    }

    fun onShare(item: MenuItem) {
        model.subscriptionData.value?.let { (subscription, _) ->
            ShareCompat.IntentBuilder.from(this)
                    .setSubject(subscription.displayName)
                    .setText(subscription.url.toString())
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
        }
    }

    private fun dirty(): Boolean = titleColorModel.dirty() || credentialsModel.dirty()


    /* view model and data source */

    class SubscriptionModel(application: Application): AndroidViewModel(application) {
        private val database = AppDatabase.getInstance(application)
        private val subscriptionsDao = database.subscriptionsDao()
        private val credentialsDao = database.credentialsDao()

        var loaded = false

        val subscriptionData = MutableLiveData<Pair<Subscription, Credential?>?>()

        /**
         * Loads a given subscription from the database given its id.
         * @throws IllegalArgumentException If there's no subscription with the given id.
         */
        fun loadSubscription(id: Long) = viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                val subscription = subscriptionsDao.getById(id) ?: throw IllegalArgumentException("There is no subscription stored with id $id")
                val credential = credentialsDao.getBySubscriptionId(id)
                subscription to credential
            }
            subscriptionData.postValue(data)
        }

        /**
         * Updates the loaded subscription from the data provided by the view models.
         * @throws IllegalStateException If [loadSubscription] has not been loaded, or was not successful.
         */
        fun updateSubscription(
            titleColorModel: TitleColorFragment.TitleColorModel,
            credentialsModel: CredentialsFragment.CredentialsModel
        ) = viewModelScope.launch {
            // Use withContext instead of passing context to launch for having correct thread in invokeOnCompletion
            withContext(Dispatchers.IO) {
                val (subscription, credential) = subscriptionData.value ?: throw IllegalStateException("Subscription not loaded")

                val newSubscription = subscription.copy(
                    displayName = titleColorModel.title.value ?: subscription.displayName,
                    color = titleColorModel.color.value,
                    defaultAlarmMinutes = titleColorModel.defaultAlarmMinutes.value,
                    ignoreEmbeddedAlerts = titleColorModel.ignoreAlerts.value ?: false
                )
                subscriptionsDao.update(newSubscription)

                SyncWorker.run(getApplication(), forceResync = true)

                val newCredential = credentialsModel.let { model ->
                    // Try to remove the stored credentials if any
                    try { credentialsDao.remove(subscription.id) } catch (_: SQLException) {}

                    if (model.requiresAuth.value == true) {
                        val newCredential = Credential(
                            subscription.id,
                            model.username.value!!,
                            model.password.value!!
                        )
                        credentialsDao.create(newCredential)
                        newCredential
                    } else
                        credential
                }
                subscriptionData.postValue(newSubscription to newCredential)
            }
        }

        /**
         * Removes the loaded subscription.
         * @throws IllegalStateException If [loadSubscription] has not been loaded, or was not successful.
         */
        fun deleteSubscription() = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val (subscription, _) = subscriptionData.value ?: throw IllegalStateException("Subscription not loaded")
                credentialsDao.remove(subscription.id)
                subscriptionsDao.delete(subscription)
            }
            subscriptionData.postValue(null)
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