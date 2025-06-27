package at.bitfire.icsdroid.model

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditSubscriptionModel.EditSubscriptionModelFactory::class)
class EditSubscriptionModel @AssistedInject constructor(
    @param:ApplicationContext val context: Context,
    @Assisted private val subscriptionId: Long
): SubscriptionSettingsModel() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EditSubscriptionModelEntryPoint {
        fun appDatabase(): AppDatabase
    }

    @AssistedFactory
    interface EditSubscriptionModelFactory {
        fun create(subscriptionId: Long): EditSubscriptionModel
    }

    init {
        // Initialise view models and save their initial state
        viewModelScope.launch {
            subscriptionWithCredential.collect { data ->
                if (data != null)
                    onSubscriptionLoaded(data)
            }
        }
    }

    val db = EntryPointAccessors.fromApplication(context, EditSubscriptionModelEntryPoint::class.java).appDatabase()

    private val credentialsDao = db.credentialsDao()
    private val subscriptionsDao = db.subscriptionsDao()

    private var initialSubscription: Subscription? = null
    private var initialCredential: Credential? = null
    private var initialRequiresAuthValue: Boolean? = null

    /**
     * Whether user input is error free
     */
    val inputValid: Boolean
        @Composable
        get() = remember(uiState) {
            val title = uiState.title
            val requiresAuth = uiState.requiresAuth
            val username = uiState.username
            val password = uiState.password

            val titleOK = !title.isNullOrBlank()
            val authOK = if (requiresAuth)
                !username.isNullOrBlank() && !password.isNullOrBlank()
            else
                true
            titleOK && authOK
        }

    /**
     * Whether there are unsaved user changes
     */
    val modelsDirty: Boolean
        @Composable
        get() = remember(uiState) {
            val requiresAuth = uiState.requiresAuth

            val credentialsDirty = initialRequiresAuthValue != requiresAuth ||
                    initialCredential?.let { !equalsCredential(it) } ?: false
            val subscriptionsDirty = initialSubscription?.let {
                !equalsSubscription(it)
            } ?: false

            credentialsDirty || subscriptionsDirty
        }

    var successMessage: String? by mutableStateOf(null)
        private set

    val subscription = db.subscriptionsDao().getByIdFlow(subscriptionId)
    val subscriptionWithCredential = db.subscriptionsDao().getWithCredentialsByIdFlow(subscriptionId)

    /**
     * Initialise view models and remember their initial state
     */
    private fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) {
        val subscription = subscriptionWithCredential.subscription

        setUrl(subscription.url.toString())
        subscription.displayName.let {
            setTitle(it)
        }
        subscription.color.let(::setColor)
        subscription.ignoreEmbeddedAlerts.let(::setIgnoreAlerts)
        subscription.defaultAlarmMinutes?.toString().let(::setDefaultAlarmMinutes)
        subscription.defaultAllDayAlarmMinutes?.toString()?.let(::setDefaultAllDayAlarmMinutes)
        subscription.ignoreDescription.let(::setIgnoreDescription)

        val credential = subscriptionWithCredential.credential
        val requiresAuth = credential != null
        setRequiresAuth(requiresAuth)

        if (credential != null) {
            credential.username.let(::setUsername)
            credential.password.let(::setPassword)
        }

        // Save state, before user makes changes
        initialSubscription = subscription
        initialCredential = credential
        initialRequiresAuthValue = uiState.requiresAuth
    }

    /**
     * Updates the loaded subscription from the data provided by the view models.
     */
    fun updateSubscription() {
        viewModelScope.launch(Dispatchers.IO) {
            subscription.firstOrNull()?.let { subscription ->
                val newSubscription = subscription.copy(
                    displayName = uiState.title ?: subscription.displayName,
                    color = uiState.color,
                    defaultAlarmMinutes = uiState.defaultAlarmMinutes,
                    defaultAllDayAlarmMinutes = uiState.defaultAllDayAlarmMinutes,
                    ignoreEmbeddedAlerts = uiState.ignoreAlerts,
                    ignoreDescription = uiState.ignoreDescription
                )
                subscriptionsDao.update(newSubscription)

                if (uiState.requiresAuth) {
                    val username = uiState.username
                    val password = uiState.password
                    if (username != null && password != null)
                        credentialsDao.upsert(Credential(subscriptionId, username, password))
                } else
                    credentialsDao.removeBySubscriptionId(subscriptionId)

                // notify UI about success
                successMessage = context.getString(R.string.edit_calendar_saved)

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(context, forceResync = true)
            } ?: Log.w(Constants.TAG, "There's no subscription to update")
        }
    }

    /**
     * Removes the loaded subscription.
     */
    fun removeSubscription() {
        viewModelScope.launch(Dispatchers.IO) {
            subscription.firstOrNull()?.let { subscription ->
                subscriptionsDao.delete(subscription)

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(context)

                // notify UI about success
                successMessage = context.getString(R.string.edit_calendar_deleted)
            } ?: Log.w(Constants.TAG, "There's no subscription to remove")
        }
    }

}