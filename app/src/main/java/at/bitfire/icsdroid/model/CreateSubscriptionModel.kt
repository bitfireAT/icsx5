package at.bitfire.icsdroid.model

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.BaseSyncWorker
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import java.net.URI
import java.net.URISyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl

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
                    displayName = subscriptionSettingsModel.uiState.title!!,
                    url = Uri.parse(subscriptionSettingsModel.uiState.url),
                    color = subscriptionSettingsModel.uiState.color,
                    ignoreEmbeddedAlerts = subscriptionSettingsModel.uiState.ignoreAlerts,
                    defaultAlarmMinutes = subscriptionSettingsModel.uiState.defaultAlarmMinutes,
                    defaultAllDayAlarmMinutes = subscriptionSettingsModel.uiState.defaultAllDayAlarmMinutes,
                    ignoreDescription = subscriptionSettingsModel.uiState.ignoreDescription,
                )

                /** A list of all the ids of the inserted rows */
                val id = subscriptionsDao.add(subscription)

                // Create the credential in the IO thread
                if (credentialsModel.uiState.requiresAuth) {
                    // If the subscription requires credentials, create them
                    val username = credentialsModel.uiState.username
                    val password = credentialsModel.uiState.password
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
                BaseSyncWorker.run(getApplication())

                uiState = uiState.copy(success = true)
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Couldn't create calendar", e)
                uiState = uiState.copy(errorMessage = e.localizedMessage ?: e.message)
            } finally {
                uiState = uiState.copy(isCreating = false)
            }
        }
    }

    private fun validateUri(
        url: String?,
        onSetUrl: (String) -> Unit,
        requiresAuth: Boolean,
        onSetRequiresAuth: (Boolean) -> Unit,
        onSetCredentials: (username: String?, password: String?) -> Unit,
        onSetIsInsecure: (Boolean) -> Unit,
        onSetUrlError: (String?) -> Unit
    ): Uri? {
        var errorMsg: String? = null

        var uri: Uri
        try {
            try {
                uri = Uri.parse(url ?: return null)
            } catch (e: URISyntaxException) {
                Log.d(Constants.TAG, "Invalid URL", e)
                errorMsg = e.localizedMessage
                return null
            }

            Log.i(Constants.TAG, uri.toString())

            if (uri.scheme.equals("webcal", true)) {
                uri = uri.buildUpon().scheme("http").build()
                onSetUrl(uri.toString())
                return null
            } else if (uri.scheme.equals("webcals", true)) {
                uri = uri.buildUpon().scheme("https").build()
                onSetUrl(uri.toString())
                return null
            }

            when (uri.scheme?.lowercase()) {
                "content" -> {
                    // SAF file, no need for auth
                }

                "http", "https" -> {
                    // check whether the URL is valid
                    try {
                        uri.toString().toHttpUrl()
                    } catch (e: IllegalArgumentException) {
                        Log.w(Constants.TAG, "Invalid URI", e)
                        errorMsg = e.localizedMessage
                        return null
                    }

                    // extract user name and password from URL
                    uri.userInfo?.let { userInfo ->
                        val credentials = userInfo.split(':')
                        onSetRequiresAuth(true)
                        onSetCredentials(
                            credentials.elementAtOrNull(0),
                            credentials.elementAtOrNull(1)
                        )

                        val urlWithoutPassword =
                            URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, null)
                        onSetUrl(urlWithoutPassword.toString())
                        return null
                    }
                }

                else -> {
                    errorMsg = getApplication<Application>().getString(R.string.add_calendar_need_valid_uri)
                    return null
                }
            }

            // warn if auth. required and not using HTTPS
            onSetIsInsecure(
                requiresAuth && !uri.scheme.equals("https", true)
            )
        } finally {
            onSetUrlError(errorMsg)
        }
        return uri
    }

    fun checkUrlIntroductionPage(
        subscriptionSettingsModel: SubscriptionSettingsModel,
        validationModel: ValidationModel,
        credentialsModel: CredentialsModel
    ) {
        if (validationModel.uiState.isVerifyingUrl) {
            setShowNextButton(true)
        } else {
            val uri = validateUri(
                url = subscriptionSettingsModel.uiState.url,
                onSetUrl = subscriptionSettingsModel::setUrl,
                onSetCredentials = { username, password ->
                    credentialsModel.setUsername(username)
                    credentialsModel.setPassword(password)
                },
                requiresAuth = credentialsModel.uiState.requiresAuth,
                onSetRequiresAuth = credentialsModel::setRequiresAuth,
                onSetIsInsecure = credentialsModel::setIsInsecure,
                onSetUrlError = subscriptionSettingsModel::setUrlError
            )
            val authOK =
                if (credentialsModel.uiState.requiresAuth)
                    !credentialsModel.uiState.username.isNullOrEmpty() &&
                            !credentialsModel.uiState.password.isNullOrEmpty()
                else
                    true
            setShowNextButton(uri != null && authOK)
        }
    }
}