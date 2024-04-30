package at.bitfire.icsdroid.model

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.net.URISyntaxException

class SubscriptionSettingsModel : ViewModel() {
    val url = MutableStateFlow<String?>(null)
    val fileName = MutableStateFlow<String?>(null)
    val urlError = MutableStateFlow<String?>(null)
    val title = MutableStateFlow<String?>(null)
    val color = MutableStateFlow<Int?>(null)
    val ignoreAlerts = MutableStateFlow(false)
    val defaultAlarmMinutes = MutableStateFlow<Long?>(null)
    val defaultAllDayAlarmMinutes = MutableStateFlow<Long?>(null)

    // advanced settings
    val ignoreDescription = MutableStateFlow(false)

    // computed settings
    val supportsAuthentication: StateFlow<Boolean> = url.map { url ->
        val uri = try {
                Uri.parse(url)
            } catch (e: URISyntaxException) {
                return@map false
            } catch (_: NullPointerException) {
                return@map false
        }
        return@map HttpUtils.supportsAuthentication(uri)
    }.stateIn(
        initialValue = false,
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    fun equalsSubscription(subscription: Subscription) =
        url.value == subscription.url.toString()
            && title.value == subscription.displayName
            && color.value == subscription.color
            && ignoreAlerts.value == subscription.ignoreEmbeddedAlerts
            && defaultAlarmMinutes.value == subscription.defaultAlarmMinutes
            && defaultAllDayAlarmMinutes.value == subscription.defaultAllDayAlarmMinutes
            && ignoreDescription.value == subscription.ignoreDescription
}