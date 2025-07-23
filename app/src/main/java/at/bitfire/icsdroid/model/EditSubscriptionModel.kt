/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.model

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = EditSubscriptionModel.EditSubscriptionModelFactory::class)
class EditSubscriptionModel @AssistedInject constructor(
    private val db: AppDatabase,
    @param:ApplicationContext val context: Context,
    @Assisted private val subscriptionId: Long,
    val subscriptionSettingsModel: SubscriptionSettingsModel
): ViewModel() {

    @AssistedFactory
    interface EditSubscriptionModelFactory {
        fun create(subscriptionId: Long): EditSubscriptionModel
    }

    private var initialSubscription: Subscription? = null
    private var initialCredential: Credential? = null
    private var initialRequiresAuthValue: Boolean? = null

    /**
     * Whether user input is error free
     */
    val inputValid: Boolean
        get() = with(subscriptionSettingsModel) {
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
        get() = with(subscriptionSettingsModel) {
            val requiresAuth = uiState.requiresAuth

            val credentialsDirty = initialRequiresAuthValue != requiresAuth || initialCredential?.let {
                !equalsCredential(it)
            } ?: false
            val subscriptionsDirty = initialSubscription?.let {
                !equalsSubscription(it)
            } ?: false

            credentialsDirty || subscriptionsDirty
        }

    var successMessage: String? by mutableStateOf(null)
        private set

    val subscription = db.subscriptionsDao().getByIdFlow(subscriptionId)
    val subscriptionWithCredential = db.subscriptionsDao().getWithCredentialsByIdFlow(subscriptionId)

    init {
        // Initialise view models and save their initial state
        viewModelScope.launch {
            subscriptionWithCredential.collect { data ->
                if (data != null)
                    onSubscriptionLoaded(data)
            }
        }
    }

    /**
     * Initialise view models and remember their initial state
     */
    private fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) =
        with(subscriptionSettingsModel) {
            val subscription = subscriptionWithCredential.subscription

            setUrl(subscription.url.toString())
            setTitle(subscription.displayName)
            setColor(subscription.color)
            setIgnoreAlerts(subscription.ignoreEmbeddedAlerts)
            setDefaultAlarmMinutes(subscription.defaultAlarmMinutes?.toString())
            setDefaultAllDayAlarmMinutes(subscription.defaultAllDayAlarmMinutes?.toString())
            setIgnoreDescription(subscription.ignoreDescription)

            val credential = subscriptionWithCredential.credential
            val requiresAuth = credential != null
            setRequiresAuth(requiresAuth)

            if (credential != null) {
                setUsername(credential.username)
                setPassword(credential.password)
            }

            // Save state, before user makes changes
            initialSubscription = subscription
            initialCredential = credential
            initialRequiresAuthValue = uiState.requiresAuth
        }

    /**
     * Updates the loaded subscription from the data provided by the view models.
     */
    fun updateSubscription() = with(subscriptionSettingsModel.uiState) {
        viewModelScope.launch(Dispatchers.IO) {
            subscription.firstOrNull()?.let { subscription ->
                val newSubscription = subscription.copy(
                    displayName = title ?: subscription.displayName,
                    color = color,
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    ignoreEmbeddedAlerts = ignoreAlerts,
                    ignoreDescription = ignoreDescription
                )
                db.subscriptionsDao().update(newSubscription)

                if (requiresAuth) {
                    if (username != null && password != null)
                        db.credentialsDao().upsert(Credential(subscriptionId, username, password))
                } else
                    db.credentialsDao().removeBySubscriptionId(subscriptionId)

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
                db.subscriptionsDao().delete(subscription)

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(context)

                // notify UI about success
                successMessage = context.getString(R.string.edit_calendar_deleted)
            } ?: Log.w(Constants.TAG, "There's no subscription to remove")
        }
    }

}