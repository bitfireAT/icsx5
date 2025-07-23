package at.bitfire.icsdroid.model

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionSettingsModel @Inject constructor() {
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

    var uiState by mutableStateOf(UiState())
        private set

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