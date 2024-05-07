/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui.views

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ShareCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.model.CredentialsModel
import at.bitfire.icsdroid.model.EditSubscriptionModel
import at.bitfire.icsdroid.model.SubscriptionSettingsModel
import at.bitfire.icsdroid.ui.screen.EditCalendarScreen
import at.bitfire.icsdroid.ui.theme.setContentThemed
import kotlinx.coroutines.launch

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
    private val inputValid: Boolean
        @Composable
        get() = remember(subscriptionSettingsModel.uiState, credentialsModel.uiState) {
            val title = subscriptionSettingsModel.uiState.title
            val requiresAuth = credentialsModel.uiState.requiresAuth
            val username = credentialsModel.uiState.username
            val password = credentialsModel.uiState.password

            val titleOK = !title.isNullOrBlank()
            val authOK = if (requiresAuth)
                !username.isNullOrBlank() && !password.isNullOrBlank()
            else
                true
            titleOK && authOK
        }

    // Whether unsaved changes exist
    private val modelsDirty: Boolean
        @Composable
        get() = remember(subscriptionSettingsModel.uiState, credentialsModel.uiState) {
            val requiresAuth = credentialsModel.uiState.requiresAuth

            val credentialsDirty = initialRequiresAuthValue != requiresAuth ||
                    initialCredential?.let { !credentialsModel.equalsCredential(it) } ?: false
            val subscriptionsDirty = initialSubscription?.let {
                !subscriptionSettingsModel.equalsSubscription(it)
            } ?: false

            credentialsDirty || subscriptionsDirty
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialise view models and save their initial state
        lifecycleScope.launch {
            model.subscriptionWithCredential.flowWithLifecycle(lifecycle).collect { data ->
                if (data != null)
                    onSubscriptionLoaded(data)
            }
        }

        setContentThemed {
            val successMessage = model.uiState.successMessage
            // show success message
            successMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                finish()
            }

            EditCalendarScreen(
                subscriptionSettingsModel,
                credentialsModel,
                inputValid,
                modelsDirty,
                { onDelete() },
                { onSave() },
                { onShare() },
                { finish() }
            )
        }
    }

    /**
     * Initialise view models and remember their initial state
     */
    private fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) {
        val subscription = subscriptionWithCredential.subscription

        subscriptionSettingsModel.setUrl(subscription.url.toString())
        subscription.displayName.let {
            subscriptionSettingsModel.setTitle(it)
        }
        subscription.color.let(subscriptionSettingsModel::setColor)
        subscription.ignoreEmbeddedAlerts.let(subscriptionSettingsModel::setIgnoreAlerts)
        subscription.defaultAlarmMinutes?.toString().let(subscriptionSettingsModel::setDefaultAlarmMinutes)
        subscription.defaultAllDayAlarmMinutes?.toString()?.let(subscriptionSettingsModel::setDefaultAllDayAlarmMinutes)
        subscription.ignoreDescription.let(subscriptionSettingsModel::setIgnoreDescription)

        val credential = subscriptionWithCredential.credential
        val requiresAuth = credential != null
        credentialsModel.setRequiresAuth(requiresAuth)

        if (credential != null) {
            credential.username.let(credentialsModel::setUsername)
            credential.password.let(credentialsModel::setPassword)
        }

        // Save state, before user makes changes
        initialSubscription = subscription
        initialCredential = credential
        initialRequiresAuthValue = credentialsModel.uiState.requiresAuth
    }


    /* user actions */

    private fun onSave() = model.updateSubscription(subscriptionSettingsModel, credentialsModel)

    private fun onDelete() = model.removeSubscription()

    private fun onShare() {
        lifecycleScope.launch {
            model.subscriptionWithCredential.value?.let { (subscription, _) ->
                ShareCompat.IntentBuilder(this@EditCalendarActivity)
                    .setSubject(subscription.displayName)
                    .setText(subscription.url.toString())
                    .setType("text/plain")
                    .setChooserTitle(R.string.edit_calendar_send_url)
                    .startChooser()
            }
        }
    }

}