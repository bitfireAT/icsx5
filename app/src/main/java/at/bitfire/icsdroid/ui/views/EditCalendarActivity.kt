/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.EditSubscriptionModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.ui.partials.ExtendedTopAppBar
import at.bitfire.icsdroid.ui.partials.GenericAlertDialog
import at.bitfire.icsdroid.ui.theme.setContentThemed
import kotlinx.coroutines.flow.combine

class EditCalendarActivity: AppCompatActivity() {

    companion object {
        const val EXTRA_SUBSCRIPTION_ID = "subscriptionId"
    }

    private val subscriptionSettingsModel by viewModels<SubscriptionSettingsModel>()
    private var initialSubscription: Subscription? = null
    private val credentialsModel by viewModels<CredentialsModel>()
    private var initialCredential: Credential? = null
    private var initialRequiresAuthValue: Boolean? = null

    // Whether user made changes are legal
    private val inputValid by lazy {
        combine(
            subscriptionSettingsModel.title,
            credentialsModel.requiresAuth,
            credentialsModel.username,
            credentialsModel.password
        ) { title, requiresAuth, username, password ->
            val titleOK = !title.isNullOrBlank()
            val authOK = credentialsModel.run {
                if (requiresAuth)
                    !username.isNullOrBlank() && !password.isNullOrBlank()
                else
                    true
            }
            titleOK && authOK
        }
    }

    // Whether unsaved changes exist
    private val modelsDirty by lazy {
        combine(
            combine(
                credentialsModel.requiresAuth,
                credentialsModel.username,
                credentialsModel.password
            ) { requiresAuth, _, _ ->
                initialRequiresAuthValue != requiresAuth ||
                        initialCredential?.let { !credentialsModel.equalsCredential(it) } ?: false
            },
            combine(
                subscriptionSettingsModel.title,
                subscriptionSettingsModel.color,
                subscriptionSettingsModel.ignoreAlerts,
                subscriptionSettingsModel.defaultAlarmMinutes,
                subscriptionSettingsModel.defaultAllDayAlarmMinutes,
                subscriptionSettingsModel.ignoreDescription
            ) {
                initialSubscription?.let {
                    !subscriptionSettingsModel.equalsSubscription(it)
                } ?: false
            }) { credentialsDirty, subscriptionsDirty ->
            credentialsDirty || subscriptionsDirty
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

        setContentThemed {
            val successMessage by model.successMessage
            // show success message
            successMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContentThemed {
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
            subscriptionSettingsModel.ignoreAlerts.value = it
        }
        subscription.defaultAlarmMinutes.let {
            subscriptionSettingsModel.defaultAlarmMinutes.value = it
        }
        subscription.defaultAllDayAlarmMinutes.let {
            subscriptionSettingsModel.defaultAllDayAlarmMinutes.value = it
        }
        subscription.ignoreDescription.let {
            subscriptionSettingsModel.ignoreDescription.value = it
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
        val url by subscriptionSettingsModel.url.collectAsStateWithLifecycle()
        val title by subscriptionSettingsModel.title.collectAsStateWithLifecycle()
        val color by subscriptionSettingsModel.color.collectAsStateWithLifecycle()
        val ignoreAlerts by subscriptionSettingsModel.ignoreAlerts.collectAsStateWithLifecycle()
        val defaultAlarmMinutes by subscriptionSettingsModel.defaultAlarmMinutes.collectAsStateWithLifecycle()
        val defaultAllDayAlarmMinutes by subscriptionSettingsModel.defaultAllDayAlarmMinutes.collectAsStateWithLifecycle()
        val ignoreDescription by subscriptionSettingsModel.ignoreDescription.collectAsStateWithLifecycle()
        val inputValid by inputValid.collectAsStateWithLifecycle(false)
        val modelsDirty by modelsDirty.collectAsStateWithLifecycle(false)
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
                    titleChanged = { subscriptionSettingsModel.title.value = it },
                    color = color,
                    colorChanged = { subscriptionSettingsModel.color.value = it },
                    ignoreAlerts = ignoreAlerts,
                    ignoreAlertsChanged = { subscriptionSettingsModel.ignoreAlerts.value = it },
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAlarmMinutesChanged = {
                        subscriptionSettingsModel.defaultAlarmMinutes.value = it.toLongOrNull()
                    },
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    defaultAllDayAlarmMinutesChanged = {
                        subscriptionSettingsModel.defaultAllDayAlarmMinutes.value = it.toLongOrNull()
                    },
                    ignoreDescription = ignoreDescription,
                    onIgnoreDescriptionChanged = {
                        subscriptionSettingsModel.ignoreDescription.value = it
                    },
                    isCreating = false,
                    modifier = Modifier.fillMaxWidth()
                )
                val supportsAuthentication by subscriptionSettingsModel.supportsAuthentication.collectAsStateWithLifecycle()
                val requiresAuth: Boolean by credentialsModel.requiresAuth.collectAsStateWithLifecycle()
                val username: String? by credentialsModel.username.collectAsStateWithLifecycle()
                val password: String? by credentialsModel.password.collectAsStateWithLifecycle()
                AnimatedVisibility(visible = supportsAuthentication) {
                    LoginCredentialsComposable(
                        requiresAuth,
                        username,
                        password,
                        onRequiresAuthChange = { credentialsModel.requiresAuth.value = it },
                        onUsernameChange = { credentialsModel.username.value = it },
                        onPasswordChange = { credentialsModel.password.value = it }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
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
        ExtendedTopAppBar(
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