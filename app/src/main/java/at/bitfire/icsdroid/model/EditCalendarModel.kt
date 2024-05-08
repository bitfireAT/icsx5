package at.bitfire.icsdroid.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.launch

class EditCalendarModel(
    val editSubscriptionModel: EditSubscriptionModel,
    val subscriptionSettingsModel: SubscriptionSettingsModel,
    val credentialsModel: CredentialsModel
): ViewModel() {

    private var initialSubscription: Subscription? = null
    private var initialCredential: Credential? = null
    private var initialRequiresAuthValue: Boolean? = null

    init {
        // Initialise view models and save their initial state
        viewModelScope.launch {
            editSubscriptionModel.subscriptionWithCredential.collect { data ->
                if (data != null)
                    onSubscriptionLoaded(data)
            }
        }
    }

    val inputValid: Boolean
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

    val modelsDirty: Boolean
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

    /**
     * Initialise view models and remember their initial state
     */
    fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) {
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

    fun onSave() = editSubscriptionModel.updateSubscription(subscriptionSettingsModel, credentialsModel)

    fun onDelete() = editSubscriptionModel.removeSubscription()
}
