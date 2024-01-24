/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.google.accompanist.themeadapter.material.MdcTheme

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
        const val EXTRA_ERROR_MESSAGE = "errorMessage"
        const val EXTRA_THROWABLE = "errorThrowable"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private var initialSubscription: Subscription? = null
    private val credentialsModel by viewModels<CredentialsModel>()
    private var initialCredentials: Credential? = null

    private val colorPickerContract = registerForActivityResult(ColorPickerActivity.Contract()) { color ->
        subscriptionSettingsModel.color.value = color
    }

    // Whether use made changes are legal
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
                        username.value != null && password.value != null
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
                addSource(credentialsModel.username) { value = credentialDirty() }
                addSource(credentialsModel.password) { value = credentialDirty() }
            }
            fun subscriptionDirty() = initialSubscription?.let {
                !subscriptionSettingsModel.equalsSubscription(it)
            } ?: false
            fun credentialDirty() = initialCredentials?.let {
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

        setContent {
            MdcTheme {
                // show error message from calling intent, if available
                if (inState == null)
                    intent.getStringExtra(EXTRA_ERROR_MESSAGE)?.let { error ->
                        AlertFragmentDialog(error, intent.getSerializableExtra(EXTRA_THROWABLE) as? Throwable) {}
                    }
                EditCalendarComposable()
            }
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
        initialCredentials = credential
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
        val color by subscriptionSettingsModel.color.observeAsState(0)
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
                    colorIconClicked = { colorPickerContract.launch(color) },
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
        val openDeleteDialog = remember { mutableStateOf(false) }
        AlertDialogBox(
            isOpen = openDeleteDialog,
            message = stringResource(R.string.edit_calendar_really_delete),
            confirmButtonText = stringResource(R.string.edit_calendar_delete),
            confirmButtonCallback = { onDelete() },
            dismissButtonText = stringResource(R.string.edit_calendar_cancel)
        )
        val openSaveDismissDialog = remember { mutableStateOf(false) }
        AlertDialogBox(
            isOpen = openSaveDismissDialog,
            message = stringResource(R.string.edit_calendar_unsaved_changes),
            confirmButtonText = stringResource(R.string.edit_calendar_save),
            confirmButtonCallback = { onSave() },
            dismissButtonText = stringResource(R.string.edit_calendar_dismiss),
            dismissButtonCallback = { finish() }
        )
        TopAppBar(
            navigationIcon = {
                IconButton(
                    onClick = {
                        if (modelsDirty)
                            openSaveDismissDialog.value = true
                        else
                            finish()
                    }
                ) {
                    Icon(Icons.Filled.ArrowBack, null)
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
                IconButton(onClick = { openDeleteDialog.value = true }) {
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

    @Composable
    fun AlertDialogBox(
        isOpen: MutableState<Boolean>,
        message: String,
        confirmButtonText: String,
        confirmButtonCallback: () -> Unit = {},
        dismissButtonText: String,
        dismissButtonCallback: () -> Unit = {}
    ) {
        if (isOpen.value)
            AlertDialog(
                onDismissRequest = { isOpen.value = false },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = {
                        isOpen.value = false
                        confirmButtonCallback()
                    }) { Text(confirmButtonText) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        isOpen.value = false
                        dismissButtonCallback()
                    }) { Text(dismissButtonText) }
                }
            )
    }

    @Preview
    @Composable
    fun TopBarComposable_Preview() {
        AppBarComposable(valid = true, modelsDirty = true)
    }

}