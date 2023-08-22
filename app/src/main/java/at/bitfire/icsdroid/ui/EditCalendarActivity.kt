/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.logic.BackHandlerCompat
import at.bitfire.icsdroid.ui.reusable.AppBarIcon
import at.bitfire.icsdroid.ui.reusable.AppBarMenu
import at.bitfire.icsdroid.ui.subscription.SubscriptionCredentialsModel
import at.bitfire.icsdroid.ui.subscription.SubscriptionSettingsModel
import com.google.accompanist.themeadapter.material.MdcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditCalendarActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
        private const val EXTRA_ERROR_MESSAGE = "errorMessage"
        private const val EXTRA_THROWABLE = "errorThrowable"

        private const val RESULT_CREATED = 2
        private const val RESULT_DELETED = 3
    }

    enum class Result(
        val code: Int,
        @StringRes val message: Int?
    ) {
        CREATED(RESULT_CREATED, R.string.edit_calendar_saved),
        DELETED(RESULT_DELETED, R.string.edit_calendar_deleted),
        CANCELLED(Activity.RESULT_CANCELED, null)
    }

    data class Data(
        val subscription: Subscription,
        val errorMessage: String? = null,
        val errorThrowable: Throwable? = null
    )

    object Contract : ActivityResultContract<Data, Result>() {
        override fun createIntent(context: Context, input: Data): Intent =
            Intent(context, EditCalendarActivity::class.java).apply {
                putExtra(EXTRA_SUBSCRIPTION_ID, input.subscription.id)
                putExtra(EXTRA_ERROR_MESSAGE, input.errorMessage)
                putExtra(EXTRA_THROWABLE, input.errorThrowable)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): Result = when (resultCode) {
            Activity.RESULT_CANCELED -> Result.CANCELLED
            RESULT_CREATED -> Result.CREATED
            RESULT_DELETED -> Result.DELETED
            else -> throw IllegalStateException("Activity returned an invalid result")
        }
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private val credentialsModel by viewModels<SubscriptionCredentialsModel>()

    private val model by viewModels<SubscriptionModel> {
        object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1)
                return SubscriptionModel(application, subscriptionId) as T
            }
        }
    }

    private val isDirty by lazy {
        MediatorLiveData<Boolean>().apply {
            addSource(subscriptionSettingsModel.isDirty) {
                value = subscriptionSettingsModel.isDirty.value == true ||
                    credentialsModel.isDirty.value == true
            }
            addSource(credentialsModel.isDirty) {
                value = subscriptionSettingsModel.isDirty.value == true ||
                    credentialsModel.isDirty.value == true
            }
        }
    }

    private val isOK by lazy {
        MediatorLiveData<Boolean>().apply {
            fun update() {
                val titleOK = !subscriptionSettingsModel.title.value.isNullOrBlank()
                val authOK = if (credentialsModel.requiresAuth.value == true)
                    credentialsModel.username.value != null && credentialsModel.password.value != null
                else
                    true
                value = titleOK && authOK
            }

            addSource(subscriptionSettingsModel.title) { update() }
            addSource(credentialsModel.requiresAuth) { update() }
            addSource(credentialsModel.username) { update() }
            addSource(credentialsModel.password) { update() }
        }
    }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        model.subscriptionWithCredential.observe(this) { data ->
            if (data != null)
                onSubscriptionLoaded(data)
        }

        // handle status changes
        model.result.observe(this) { result ->
            if (result != null) {
                setResult(result.code)
                finish()
            }
        }

        // show error message from calling intent, if available
        if (inState == null)
            intent.getStringExtra(EXTRA_ERROR_MESSAGE)?.let { error ->
                AlertFragment.create(error, intent.getSerializableExtra(EXTRA_THROWABLE) as? Throwable)
                    .show(supportFragmentManager, null)
            }

        setContent {
            MdcTheme {
                val isDirty by isDirty.observeAsState(initial = false)

                BackHandlerCompat { onBack(isDirty) }

                Scaffold(
                    topBar = { TopBar(isDirty) }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Content()
                    }
                }
            }
        }
    }

    @Composable
    fun ColumnScope.Content() {
        /*
        // if local file, hide authentication fragment
        val uri = model.subscriptionWithCredential.value?.subscription?.url
        binding.credentials.visibility =
            if (uri != null && HttpUtils.supportsAuthentication(uri))
                View.VISIBLE
            else
                View.GONE
         */

        SubscriptionSettings(subscriptionSettingsModel)
    }

    @Composable
    fun TopBar(isDirty: Boolean) {
        val isOK by isOK.observeAsState(true)

        TopAppBar(
            title = { Text(stringResource(R.string.activity_edit_calendar)) },
            navigationIcon = {
                IconButton(onClick = { onBack(isDirty) }) {
                    Icon(Icons.Rounded.ArrowBack, stringResource(R.string.action_back))
                }
            },
            actions = {
                AppBarMenu(
                    icons = listOf(
                        // FIXME - I think this is redundant since there's already a back arrow
                        /*AppBarIcon(
                            Icons.Rounded.Close,
                            stringResource(R.string.edit_calendar_cancel),
                            ::onCancel
                        ),*/
                        AppBarIcon(
                            Icons.Rounded.Delete,
                            stringResource(R.string.edit_calendar_delete),
                            ::onAskDelete
                        ) { (isDirty, _) -> !isDirty },
                        AppBarIcon(
                            Icons.Rounded.Check,
                            stringResource(R.string.edit_calendar_save),
                            ::onSave
                        ) { (isDirty, isOK) -> isDirty && isOK },
                        AppBarIcon(
                            Icons.Rounded.Share,
                            stringResource(R.string.edit_calendar_send_url),
                            ::onShare
                        )
                    ),
                    value = isDirty to isOK
                )
            }
        )
    }

    private fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) {
        val subscription = subscriptionWithCredential.subscription

        subscriptionSettingsModel.url.value = subscription.url.toString()
        subscription.displayName.let {
            subscriptionSettingsModel.originalTitle = it
            subscriptionSettingsModel.title.value = it
        }
        subscription.color.let {
            subscriptionSettingsModel.originalColor = it
            subscriptionSettingsModel.color.value = it
        }
        subscription.ignoreEmbeddedAlerts.let {
            subscriptionSettingsModel.originalIgnoreAlerts = it
            subscriptionSettingsModel.ignoreAlerts.postValue(it)
        }
        subscription.defaultAlarmMinutes.let {
            subscriptionSettingsModel.originalDefaultAlarmMinutes = it
            subscriptionSettingsModel.defaultAlarmMinutes.postValue(it)
        }
        subscription.defaultAllDayAlarmMinutes.let {
            subscriptionSettingsModel.originalDefaultAllDayAlarmMinutes = it
            subscriptionSettingsModel.defaultAllDayAlarmMinutes.postValue(it)
        }

        val credential = subscriptionWithCredential.credential
        val requiresAuth = credential != null
        credentialsModel.originalRequiresAuth = requiresAuth
        credentialsModel.requiresAuth.value = requiresAuth

        if (credential != null) {
            credential.username.let { username ->
                credentialsModel.originalUsername = username
                credentialsModel.username.value = username
            }
            credential.password.let { password ->
                credentialsModel.originalPassword = password
                credentialsModel.password.value = password
            }
        }
    }


    /* user actions */

    private fun onBack(isDirty: Boolean) {
        if (isDirty) {
            // If the form is dirty, warn the user about losing changes
            supportFragmentManager.beginTransaction()
                .add(SaveDismissDialogFragment(), null)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
        } else {
            // Otherwise, simply finish the activity
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun onSave() {
        model.updateSubscription(subscriptionSettingsModel, credentialsModel)
    }

    fun onCancel() {
        model.result.postValue(Result.CANCELLED)
    }

    private fun onAskDelete() {
        supportFragmentManager.beginTransaction()
            .add(DeleteDialogFragment(), null)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .commit()
    }

    private fun onDelete() {
        model.removeSubscription()
    }

    private fun onShare() {
        model.subscriptionWithCredential.value?.let { (subscription, _) ->
            ShareCompat.IntentBuilder(this)
                    .setSubject(subscription.displayName)
                    .setText(subscription.url.toString())
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
        }
    }


    /* view model and data source */

    class SubscriptionModel(
        application: Application,
        private val subscriptionId: Long
    ): AndroidViewModel(application) {

        private val db = AppDatabase.getInstance(application)
        private val credentialsDao = db.credentialsDao()
        private val subscriptionsDao = db.subscriptionsDao()

        val result = MutableLiveData<Result>()

        val subscriptionWithCredential = db.subscriptionsDao().getWithCredentialsByIdLive(subscriptionId)

        /**
         * Updates the loaded subscription from the data provided by the view models.
         */
        fun updateSubscription(
            subscriptionSettingsModel: SubscriptionSettingsModel,
            credentialsModel: SubscriptionCredentialsModel
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                subscriptionWithCredential.value?.let { subscriptionWithCredentials ->
                    val subscription = subscriptionWithCredentials.subscription

                    val newSubscription = subscription.copy(
                        displayName = subscriptionSettingsModel.title.value ?: subscription.displayName,
                        color = subscriptionSettingsModel.color.value,
                        defaultAlarmMinutes = subscriptionSettingsModel.defaultAlarmMinutes.value,
                        defaultAllDayAlarmMinutes = subscriptionSettingsModel.defaultAllDayAlarmMinutes.value,
                        ignoreEmbeddedAlerts = subscriptionSettingsModel.ignoreAlerts.value ?: false
                    )
                    subscriptionsDao.update(newSubscription)

                    if (credentialsModel.requiresAuth.value == true) {
                        val username = credentialsModel.username.value
                        val password = credentialsModel.password.value
                        if (username != null && password != null)
                            credentialsDao.upsert(Credential(subscriptionId, username, password))
                    } else
                        credentialsDao.removeBySubscriptionId(subscriptionId)

                    // notify UI about success
                    result.postValue(Result.CREATED)

                    // sync the subscription to reflect the changes in the calendar provider
                    SyncWorker.run(getApplication(), forceResync = true)
                }
            }
        }

        /**
         * Removes the loaded subscription.
         */
        fun removeSubscription() {
            viewModelScope.launch(Dispatchers.IO) {
                subscriptionWithCredential.value?.let { subscriptionWithCredentials ->
                    subscriptionsDao.delete(subscriptionWithCredentials.subscription)

                    // sync the subscription to reflect the changes in the calendar provider
                    SyncWorker.run(getApplication())

                    // notify UI about success
                    result.postValue(Result.DELETED)
                }
            }
        }

    }


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
                    (activity as? EditCalendarActivity)?.onSave()
                }
                .setNegativeButton(R.string.edit_calendar_dismiss) { dialog, _ ->
                    dialog.dismiss()
                    (activity as? EditCalendarActivity)?.onCancel()
                }
                .create()

    }

}