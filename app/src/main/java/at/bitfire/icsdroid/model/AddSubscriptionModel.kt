package at.bitfire.icsdroid.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.ResourceInfo
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.net.URI
import java.net.URISyntaxException

@HiltViewModel(assistedFactory = AddSubscriptionModel.Factory::class)
class AddSubscriptionModel @AssistedInject constructor(
    @Assisted("title") initialTitle: String?,
    @Assisted("color") initialColor: Int?,
    @Assisted("url") initialUrl: String?,
    @param:ApplicationContext private val context: Context,
    private val db: AppDatabase,
    val validator: Validator
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("title") title: String? = null,
            @Assisted("color") color: Int? = null,
            @Assisted("url") url: String? = null
        ): AddSubscriptionModel
    }

    val subscriptionSettingsUseCase: SubscriptionSettingsUseCase = SubscriptionSettingsUseCase(
        SubscriptionSettingsUseCase.UiState(
            title = initialTitle,
            color = initialColor,
            url = initialUrl
        )
    )

    data class UiState(
        val errorMessage: String? = null,
        val isCreating: Boolean = false,
        val showNextButton: Boolean = false,
        val isVerifyingUrl: Boolean = false,
        val verificationResult: ResourceInfo? = null
    )

    var uiState by mutableStateOf(UiState())
        private set

    fun setShowNextButton(value: Boolean) {
        uiState = uiState.copy(showNextButton = value)
    }

    fun resetValidationResult() {
        uiState = uiState.copy(verificationResult = null)
    }

    fun validateUrl(
        originalUri: Uri,
        username: String? = null,
        password: String? = null,
        customUserAgent: String? = null
    ) = viewModelScope.launch {
        uiState = uiState.copy(isVerifyingUrl = true)
        val result = validator.validate(originalUri, username, password, customUserAgent)
        uiState = uiState.copy(isVerifyingUrl = false, verificationResult = result)
    }
    
    fun checkUrlIntroductionPage() {
        if (uiState.isVerifyingUrl) {
            setShowNextButton(true)
        } else {
            with(subscriptionSettingsUseCase) {
                val uri = validateUri(
                    url = uiState.url,
                    onSetUrl = ::setUrl,
                    onSetCredentials = { username, password ->
                        setUsername(username)
                        setPassword(password)
                    },
                    requiresAuth = uiState.requiresAuth,
                    onSetRequiresAuth = ::setRequiresAuth,
                    onSetIsInsecure = ::setIsInsecure,
                    onSetUrlError = ::setUrlError
                )
                val authOK =
                    if (uiState.requiresAuth)
                        !uiState.username.isNullOrEmpty() &&
                                !uiState.password.isNullOrEmpty()
                    else
                        true
                setShowNextButton(uri != null && authOK)
            }
        }
    }

    /**
     * Creates a new subscription taking the data from the given models.
     */
    fun createSubscription() = viewModelScope.launch(Dispatchers.IO) {
        uiState = uiState.copy(isCreating = true)
        try {
            with(subscriptionSettingsUseCase.uiState) {
                val subscription = Subscription(
                    displayName = title!!,
                    url = Uri.parse(url),
                    color = color,
                    customUserAgent = customUserAgent,
                    ignoreEmbeddedAlerts = ignoreAlerts,
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    ignoreDescription = ignoreDescription,
                )

                /** A list of all the ids of the inserted rows */
                val id = db.subscriptionsDao().add(subscription)

                // Create the credential in the IO thread
                if (requiresAuth) {
                    // If the subscription requires credentials, create them
                    if (username != null && password != null) {
                        db.credentialsDao().create(
                            Credential(
                                subscriptionId = id,
                                username = username,
                                password = password
                            )
                        )
                    }
                }

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(context)
            }
            toastAsync(
                context,
                messageResId = R.string.add_calendar_created,
                cancelToast = null,
                duration = Toast.LENGTH_LONG
            )
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Couldn't create calendar", e)
            toastAsync(
                context,
                message = { e.localizedMessage ?: e.message },
                cancelToast = null,
                duration = Toast.LENGTH_LONG
            )
        } finally {
            uiState = uiState.copy(isCreating = false)
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
                    errorMsg = context.getString(R.string.add_calendar_need_valid_uri)
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

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return

        // keep the picked file accessible after the first sync and reboots
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        subscriptionSettingsUseCase.setUrl(uri.toString())

        // Get file name
        val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.getString(name)
        }
        subscriptionSettingsUseCase.setFileName(displayName)

        checkUrlIntroductionPage()
    }
}