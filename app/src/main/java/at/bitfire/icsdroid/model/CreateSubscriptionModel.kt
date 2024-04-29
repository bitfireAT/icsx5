package at.bitfire.icsdroid.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CreateSubscriptionModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(getApplication())
    private val subscriptionsDao = database.subscriptionsDao()
    private val credentialsDao = database.credentialsDao()

    data class UiState(
        val success: Boolean = false,
        val errorMessage: String? = null,
        val isCreating: Boolean = false,
        val showNextButton: Boolean = false
    )

    var uiState by mutableStateOf(UiState())
        private set

    fun setShowNextButton(value: Boolean) {
        uiState = uiState.copy(showNextButton = value)
    }

    /**
     * Creates a new subscription taking the data from the given models.
     */
    fun create(
        subscriptionSettingsModel: SubscriptionSettingsModel,
        credentialsModel: CredentialsModel,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState = uiState.copy(isCreating = true)
            try {
                val subscription = Subscription(
                    displayName = subscriptionSettingsModel.title.value!!,
                    url = Uri.parse(subscriptionSettingsModel.url.value),
                    color = subscriptionSettingsModel.color.value,
                    ignoreEmbeddedAlerts = subscriptionSettingsModel.ignoreAlerts.value,
                    defaultAlarmMinutes = subscriptionSettingsModel.defaultAlarmMinutes.value,
                    defaultAllDayAlarmMinutes = subscriptionSettingsModel.defaultAllDayAlarmMinutes.value,
                    ignoreDescription = subscriptionSettingsModel.ignoreDescription.value,
                )

                /** A list of all the ids of the inserted rows */
                val id = subscriptionsDao.add(subscription)

                // Create the credential in the IO thread
                if (credentialsModel.requiresAuth.value) {
                    // If the subscription requires credentials, create them
                    val username = credentialsModel.username.value
                    val password = credentialsModel.password.value
                    if (username != null && password != null) {
                        val credential = Credential(
                            subscriptionId = id,
                            username = username,
                            password = password
                        )
                        credentialsDao.create(credential)
                    }
                }

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(getApplication())

                uiState = uiState.copy(success = true)
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Couldn't create calendar", e)
                uiState = uiState.copy(errorMessage = e.localizedMessage ?: e.message)
            } finally {
                uiState = uiState.copy(isCreating = false)
            }
        }
    }
}