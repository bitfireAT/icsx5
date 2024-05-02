package at.bitfire.icsdroid.model

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ShareCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EditSubscriptionModel(
    application: Application,
    private val subscriptionId: Long
): AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val credentialsDao = db.credentialsDao()
    private val subscriptionsDao = db.subscriptionsDao()

    data class UiState(
        val successMessage: String? = null
    )

    var uiState by mutableStateOf(UiState())
        private set

    val subscriptionWithCredential = db.subscriptionsDao().getWithCredentialsByIdFlow(subscriptionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Updates the loaded subscription from the data provided by the view models.
     */
    fun updateSubscription(
        subscriptionSettingsModel: SubscriptionSettingsModel,
        credentialsModel: CredentialsModel
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionWithCredential.value?.let { subscriptionWithCredentials ->
                val subscription = subscriptionWithCredentials.subscription

                val newSubscription = subscription.copy(
                    displayName = subscriptionSettingsModel.title.value ?: subscription.displayName,
                    color = subscriptionSettingsModel.color.value,
                    defaultAlarmMinutes = subscriptionSettingsModel.defaultAlarmMinutes.value,
                    defaultAllDayAlarmMinutes = subscriptionSettingsModel.defaultAllDayAlarmMinutes.value,
                    ignoreEmbeddedAlerts = subscriptionSettingsModel.ignoreAlerts.value,
                    ignoreDescription = subscriptionSettingsModel.ignoreDescription.value
                )
                subscriptionsDao.update(newSubscription)

                if (credentialsModel.requiresAuth.value) {
                    val username = credentialsModel.username.value
                    val password = credentialsModel.password.value
                    if (username != null && password != null)
                        credentialsDao.upsert(Credential(subscriptionId, username, password))
                } else
                    credentialsDao.removeBySubscriptionId(subscriptionId)

                // notify UI about success
                uiState = uiState.copy(successMessage = getApplication<Application>().getString(R.string.edit_calendar_saved))

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(getApplication(), forceResync = true)
            } ?: Log.w(Constants.TAG, "There's no subscription to update")
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
                uiState = uiState.copy(successMessage = getApplication<Application>().getString(R.string.edit_calendar_deleted))
            } ?: Log.w(Constants.TAG, "There's no subscription to remove")
        }
    }

}