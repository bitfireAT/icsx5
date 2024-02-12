/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.EditSubscriptionModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.ui.dialog.AlertFragmentDialog
import at.bitfire.icsdroid.ui.dialog.GenericAlertDialog
import at.bitfire.icsdroid.ui.theme.setContentThemed

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
        const val EXTRA_ERROR_MESSAGE = "errorMessage"
        const val EXTRA_THROWABLE = "errorThrowable"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private var initialSubscription: Subscription? = null
    private val credentialsModel by viewModels<CredentialsModel>()
    private var initialCredential: Credential? = null
    private var initialRequiresAuthValue: Boolean? = null

    // Whether user made changes are legal
    private val inputValid: LiveData<Boolean> by lazy {
        object : MediatorLiveData<Boolean>() {
            init {
                addSource(subscriptionSettingsModel.title) { validate() }
                addSource(credentialsModel.requiresAuth) { validate() }
                addSource(credentialsModel.username) { validate() }
                addSource(credentialsModel.password) { validate() }
            }
            fun validate() {
                val titleOK = !subscriptionSettingsModel.title.value.isNullOrBlank()
                val authOK = credentialsModel.run {
                    if (requiresAuth.value == true)
                        !username.value.isNullOrBlank() && !password.value.isNullOrBlank()
                    else
                        true
                }
                value = titleOK && authOK
            }
        }
    }

    // Whether unsaved changes exist
    private val modelsDirty: MutableLiveData<Boolean> by lazy {
        object : MediatorLiveData<Boolean>() {
            init {
                addSource(subscriptionSettingsModel.title) { value = subscriptionDirty() }
                addSource(subscriptionSettingsModel.color) { value = subscriptionDirty() }
                addSource(subscriptionSettingsModel.ignoreAlerts) { value = subscriptionDirty() }
                addSource(subscriptionSettingsModel.defaultAlarmMinutes) { value = subscriptionDirty() }
                addSource(subscriptionSettingsModel.defaultAllDayAlarmMinutes) { value = subscriptionDirty() }
                addSource(credentialsModel.requiresAuth) { value = credentialDirty() }
                addSource(credentialsModel.username) { value = credentialDirty() }
                addSource(credentialsModel.password) { value = credentialDirty() }
            }
            fun subscriptionDirty() = initialSubscription?.let {
                !subscriptionSettingsModel.equalsSubscription(it)
            } ?: false
            fun credentialDirty() =
                initialRequiresAuthValue != credentialsModel.requiresAuth.value ||
                initialCredential?.let {
                    !credentialsModel.equalsCredential(it)
                } ?: false
        }
    }

    private val model by viewModels<EditSubscriptionModel> {
        object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val subscriptionId = intent.getLongExtra(EXTRA_SUBSCRIPTION_ID, -1)
                return EditSubscriptionModel(application, subscriptionId) as T
            }
        }
    }

    override fun onCreate(inState: Bundle?) {
        super.onCreate(inState)

        // Initialise view models and save their initial state
        model.subscriptionWithCredential.observe(this) { data ->
            if (data != null)
                onSubscriptionLoaded(data)
        }

        // handle status changes
        model.successMessage.observe(this) { message ->
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContentThemed {
            // show error message from calling intent, if available
            if (inState == null)
                intent.getStringExtra(EXTRA_ERROR_MESSAGE)?.let { error ->
                    AlertFragmentDialog(error, intent.getSerializableExtra(EXTRA_THROWABLE) as? Throwable) {}
                }
            EditCalendarComposable()
        }
    }

    /**
     * Initialise view models and remember their initial state
     */
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

        // Save state, before user makes changes
        initialSubscription = subscription
        initialCredential = credential
        initialRequiresAuthValue = credentialsModel.requiresAuth.value
    }


    /* user actions */

    private fun onSave() = model.updateSubscription(subscriptionSettingsModel, credentialsModel)

    private fun onDelete() = model.removeSubscription()

    private fun onShare() = model.subscriptionWithCredential.value?.let { (subscription, _) ->
        ShareCompat.IntentBuilder(this)
            .setSubject(subscription.displayName)
            .setText(subscription.url.toString())
            .setType("text/plain")
            .setChooserTitle(R.string.edit_calendar_send_url)
            .startChooser()
    }

    /* Composables */

    @Composable
    private fun EditCalendarComposable() {
        val url by subscriptionSettingsModel.url.observeAsState("")
        val title by subscriptionSettingsModel.title.observeAsState("")
        val color by subscriptionSettingsModel.color.observeAsState()
        val ignoreAlerts by subscriptionSettingsModel.ignoreAlerts.observeAsState(false)
        val defaultAlarmMinutes by subscriptionSettingsModel.defaultAlarmMinutes.observeAsState()
        val defaultAllDayAlarmMinutes by subscriptionSettingsModel.defaultAllDayAlarmMinutes.observeAsState()
        val inputValid by inputValid.observeAsState(false)
        val modelsDirty by modelsDirty.observeAsState(false)
        Scaffold(
            topBar = { AppBarComposable(inputValid, modelsDirty) }
        ) { paddingValues ->
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                SubscriptionSettingsComposable(
                    url = url,
                    title = title,
                    titleChanged = { subscriptionSettingsModel.title.postValue(it) },
                    color = color,
                    colorChanged = subscriptionSettingsModel.color::postValue,
                    ignoreAlerts = ignoreAlerts,
                    ignoreAlertsChanged = { subscriptionSettingsModel.ignoreAlerts.postValue(it) },
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAlarmMinutesChanged = {
                        subscriptionSettingsModel.defaultAlarmMinutes.postValue(
                            it.toLongOrNull()
                        )
                    },
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    defaultAllDayAlarmMinutesChanged = {
                        subscriptionSettingsModel.defaultAllDayAlarmMinutes.postValue(
                            it.toLongOrNull()
                        )
                    },
                    isCreating = false,
                    modifier = Modifier.fillMaxWidth()
                )
                val supportsAuthentication: Boolean by subscriptionSettingsModel.supportsAuthentication.observeAsState(
                    false
                )
                val requiresAuth: Boolean by credentialsModel.requiresAuth.observeAsState(false)
                val username: String? by credentialsModel.username.observeAsState(null)
                val password: String? by credentialsModel.password.observeAsState(null)
                AnimatedVisibility(visible = supportsAuthentication) {
                    LoginCredentialsComposable(
                        requiresAuth,
                        username,
                        password,
                        onRequiresAuthChange = credentialsModel.requiresAuth::setValue,
                        onUsernameChange = credentialsModel.username::setValue,
                        onPasswordChange = credentialsModel.password::setValue
                    )
                }
            }
        }
    }

    @Composable
    private fun AppBarComposable(valid: Boolean, modelsDirty: Boolean) {
        var openDeleteDialog by remember { mutableStateOf(false) }
        if (openDeleteDialog)
            GenericAlertDialog(
                content = { Text(stringResource(R.string.edit_calendar_really_delete)) },
                confirmButton = stringResource(R.string.edit_calendar_delete) to {
                    onDelete()
                    openDeleteDialog = false
                },
                dismissButton = stringResource(R.string.edit_calendar_cancel) to {
                    openDeleteDialog = false
                                                                                 },
            ) { openDeleteDialog = false }
        var openSaveDismissDialog by remember { mutableStateOf(false) }
        if (openSaveDismissDialog) {
            GenericAlertDialog(
                content = { Text(text = if (valid)
                    stringResource(R.string.edit_calendar_unsaved_changes)
                else
                    stringResource(R.string.edit_calendar_need_valid_credentials)
                ) },
                confirmButton = if (valid) stringResource(R.string.edit_calendar_save) to {
                    onSave()
                    openSaveDismissDialog = false
                } else stringResource(R.string.edit_calendar_edit) to {
                    openSaveDismissDialog = false
                },
                dismissButton = stringResource(R.string.edit_calendar_dismiss) to ::finish
            ) { openSaveDismissDialog = false }
        }
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (modelsDirty)
                            openSaveDismissDialog = true
                        else
                            finish()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                }
            },
            title = { Text(text = stringResource(R.string.activity_edit_calendar)) },
            actions = {
                IconButton(onClick = { onShare() }) {
                    Icon(
                        Icons.Filled.Share,
                        stringResource(R.string.edit_calendar_send_url)
                    )
                }
                IconButton(onClick = { openDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.edit_calendar_delete))
                }
                AnimatedVisibility(visible = valid && modelsDirty) {
                    IconButton(onClick = { onSave() }) {
                        Icon(Icons.Filled.Check, stringResource(R.string.edit_calendar_save))
                    }
                }
            }
        )
    }

    @Preview
    @Composable
    fun TopBarComposable_Preview() {
        AppBarComposable(valid = true, modelsDirty = true)
    }

}