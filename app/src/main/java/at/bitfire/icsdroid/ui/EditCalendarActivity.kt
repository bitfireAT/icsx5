/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.EditCalendarBinding
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.EditSubscriptionModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
        const val EXTRA_ERROR_MESSAGE = "errorMessage"
        const val EXTRA_THROWABLE = "errorThrowable"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<CredentialsModel>()

    private val model by viewModels<EditSubscriptionModel> {
        object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1)
                return EditSubscriptionModel(application, subscriptionId) as T
            }
        }
    }

    lateinit var binding: EditCalendarBinding


    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        model.subscriptionWithCredential.observe(this) { data ->
            if (data != null)
                onSubscriptionLoaded(data)
        }

        val invalidate = Observer<Any?> {
            invalidateOptionsMenu()
        }
        arrayOf(
            subscriptionSettingsModel.title,
            subscriptionSettingsModel.color,
            subscriptionSettingsModel.ignoreAlerts,
            subscriptionSettingsModel.defaultAlarmMinutes,
            subscriptionSettingsModel.defaultAllDayAlarmMinutes,
            credentialsModel.requiresAuth,
            credentialsModel.username,
            credentialsModel.password
        ).forEach { element ->
            element.observe(this, invalidate)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.edit_calendar)
        binding.lifecycleOwner = this
        binding.model = model

        // handle status changes
        model.successMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                finish()
            }
        }

        // show error message from calling intent, if available
        if (inState == null)
            intent.getStringExtra(EXTRA_ERROR_MESSAGE)?.let { error ->
                AlertFragment.create(error, intent.getSerializableExtra(EXTRA_THROWABLE) as? Throwable)
                    .show(supportFragmentManager, null)
            }

        onBackPressedDispatcher.addCallback {
            if (dirty()) {
                // If the form is dirty, warn the user about losing changes
                supportFragmentManager.beginTransaction()
                    .add(SaveDismissDialogFragment(), null)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
            } else
                // Otherwise, simply finish the activity
                finish()
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
        val uri = model.subscriptionWithCredential.value?.subscription?.url
        binding.credentials.visibility =
            if (uri != null && HttpUtils.supportsAuthentication(uri))
                View.VISIBLE
            else
                View.GONE

        val titleOK = !subscriptionSettingsModel.title.value.isNullOrBlank()
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

    private fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) {
        val subscription = subscriptionWithCredential.subscription

        subscriptionSettingsModel.url.value = subscription.url.toString()
        subscription.displayName.let {
            subscriptionSettingsModel.title.value = it
        }
        subscription.color.let {
            subscriptionSettingsModel.color.value = it
        }
        subscription.ignoreEmbeddedAlerts.let {
            subscriptionSettingsModel.ignoreAlerts.postValue(it)
        }
        subscription.defaultAlarmMinutes.let {
            subscriptionSettingsModel.defaultAlarmMinutes.postValue(it)
        }
        subscription.defaultAllDayAlarmMinutes.let {
            subscriptionSettingsModel.defaultAllDayAlarmMinutes.postValue(it)
        }

        val credential = subscriptionWithCredential.credential
        val requiresAuth = credential != null
        credentialsModel.requiresAuth.value = requiresAuth

        if (credential != null) {
            credential.username.let { username ->
                credentialsModel.username.value = username
            }
            credential.password.let { password ->
                credentialsModel.password.value = password
            }
        }
    }


    /* user actions */

    fun onSave(item: MenuItem?) {
        model.updateSubscription(subscriptionSettingsModel, credentialsModel)
    }

    fun onAskDelete(item: MenuItem) {
        supportFragmentManager.beginTransaction()
            .add(DeleteDialogFragment(), null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    private fun onDelete() {
        model.removeSubscription()
    }

    fun onCancel(item: MenuItem?) {
        finish()
    }

    fun onShare(item: MenuItem) {
        model.subscriptionWithCredential.value?.let { (subscription, _) ->
            ShareCompat.IntentBuilder(this)
                    .setSubject(subscription.displayName)
                    .setText(subscription.url.toString())
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
        }
    }

    //TODO: Get rid of dirty model mechanism
    private fun dirty(): Boolean = true

    /** "Really delete?" dialog */
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

    /** "Save or dismiss" dialog */
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

}