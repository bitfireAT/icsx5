package at.bitfire.icsdroid.model

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.URISyntaxException
import javax.inject.Inject

@HiltViewModel
class SubscriptionSettingsModel @Inject constructor() : ViewModel() {
    data class UiState(
        val url: String? = null,
        val fileName: String? = null,
        val urlError: String? = null,
        val title: String? = null,
        val color: Int? = null,
        val ignoreAlerts: Boolean = false,
        val defaultAlarmMinutes: Long? = null,
        val defaultAllDayAlarmMinutes: Long? = null,
        // advanced settings
        val ignoreDescription: Boolean = false,

        // credentials
        val requiresAuth: Boolean = false,
        val username: String? = null,
        val password: String? = null,
        val isInsecure: Boolean = false
    ) {
        // computed settings
        val supportsAuthentication: Boolean = url.let {
            val uri = try {
                Uri.parse(url)
            } catch (e: URISyntaxException) {
                return@let false
            } catch (_: NullPointerException) {
                return@let false
            }
            HttpUtils.supportsAuthentication(uri)
        }
    }

    private var initialSubscription: Subscription? = null
    private var initialCredential: Credential? = null
    private var initialRequiresAuthValue: Boolean? = null

    var uiState by mutableStateOf(UiState())
        private set

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

    /**
     * Initialise view models and remember their initial state
     */
    fun onSubscriptionLoaded(
        subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential,
    ) {
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

    fun setUrl(value: String?) {
        uiState = uiState.copy(url = value)
    }

    fun setFileName(value: String?) {
        uiState = uiState.copy(fileName = value)
    }

    fun setUrlError(value: String?) {
        uiState = uiState.copy(urlError = value)
    }

    fun setTitle(value: String) {
        uiState = uiState.copy(title = value)
    }

    fun setColor(value: Int?) {
        uiState = uiState.copy(color = value)
    }

    fun setIgnoreAlerts(value: Boolean) {
        uiState = uiState.copy(ignoreAlerts = value)
    }

    fun setDefaultAlarmMinutes(value: String?) {
        uiState = uiState.copy(defaultAlarmMinutes = value?.toLongOrNull())
    }

    fun setDefaultAllDayAlarmMinutes(value: String?) {
        uiState = uiState.copy(defaultAllDayAlarmMinutes = value?.toLongOrNull())
    }

    fun setIgnoreDescription(value: Boolean) {
        uiState = uiState.copy(ignoreDescription = value)
    }

    fun equalsSubscription(subscription: Subscription) =
        uiState.url == subscription.url.toString()
            && uiState.title == subscription.displayName
            && uiState.color == subscription.color
            && uiState.ignoreAlerts == subscription.ignoreEmbeddedAlerts
            && uiState.defaultAlarmMinutes == subscription.defaultAlarmMinutes
            && uiState.defaultAllDayAlarmMinutes == subscription.defaultAllDayAlarmMinutes
            && uiState.ignoreDescription == subscription.ignoreDescription

    fun setRequiresAuth(value: Boolean) {
        uiState = uiState.copy(requiresAuth = value)
    }

    fun setUsername(value: String?) {
        uiState = uiState.copy(username = value)
    }

    fun setPassword(value: String?) {
        uiState = uiState.copy(password = value)
    }

    fun clearCredentials() {
        uiState = uiState.copy(username = null, password = null)
    }

    fun setIsInsecure(value: Boolean) {
        uiState = uiState.copy(isInsecure = value)
    }

    fun equalsCredential(credential: Credential) =
        uiState.username == credential.username
                && uiState.password == credential.password
}