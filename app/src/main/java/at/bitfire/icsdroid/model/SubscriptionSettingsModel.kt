package at.bitfire.icsdroid.model

import android.net.Uri
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.HttpUtils
import java.net.URISyntaxException

class SubscriptionSettingsModel : ViewModel() {
    val url = MutableLiveData<String?>(null)
    val urlError = MutableLiveData<String?>(null)
    val title = MutableLiveData<String?>(null)
    val color = MutableLiveData<Int?>(null)
    val ignoreAlerts = MutableLiveData(false)
    val defaultAlarmMinutes = MutableLiveData<Long?>(null)
    val defaultAllDayAlarmMinutes = MutableLiveData<Long?>(null)

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
}