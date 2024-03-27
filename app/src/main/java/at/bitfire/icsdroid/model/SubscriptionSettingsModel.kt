package at.bitfire.icsdroid.model

import android.net.Uri
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.HttpUtils
import at.bitfire.icsdroid.db.entity.Subscription
import java.net.URISyntaxException

class SubscriptionSettingsModel : ViewModel() {
    val url = MutableLiveData<String?>(null)
    val urlError = MutableLiveData<String?>(null)
    val title = MutableLiveData<String?>(null)
    val color = MutableLiveData<Int?>(null)
    val ignoreAlerts = MutableLiveData(false)
    val defaultAlarmMinutes = MutableLiveData<Long?>(null)
    val defaultAllDayAlarmMinutes = MutableLiveData<Long?>(null)

    // advanced
    val ignoreDescription = MutableLiveData(false)

    val supportsAuthentication = MediatorLiveData(false).apply {
        addSource(url) {
            val uri = try {
                Uri.parse(it)
            } catch (e: URISyntaxException) {
                return@addSource
            } catch (_: NullPointerException) {
                return@addSource
            }
            value = HttpUtils.supportsAuthentication(uri)
        }
    }

    fun equalsSubscription(subscription: Subscription) =
        url.value == subscription.url.toString()
            && title.value == subscription.displayName
            && color.value == subscription.color
            && ignoreAlerts.value == subscription.ignoreEmbeddedAlerts
            && defaultAlarmMinutes.value == subscription.defaultAlarmMinutes
            && defaultAllDayAlarmMinutes.value == subscription.defaultAllDayAlarmMinutes
            && ignoreDescription.value == subscription.ignoreDescription
}