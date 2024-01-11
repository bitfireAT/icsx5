package at.bitfire.icsdroid.model

import android.net.Uri
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.HttpUtils
import java.net.URISyntaxException

class SubscriptionSettingsModel : ViewModel() {
    val url = MutableLiveData<String>()
    val urlError = MutableLiveData<String?>()
    val title = MutableLiveData<String>()
    val color = MutableLiveData<Int>()
    val ignoreAlerts = MutableLiveData<Boolean>()
    val defaultAlarmMinutes = MutableLiveData<Long>()
    val defaultAllDayAlarmMinutes = MutableLiveData<Long>()

    val supportsAuthentication = MediatorLiveData(false).apply {
        addSource(url) {
            val uri = try {
                Uri.parse(it)
            } catch (e: URISyntaxException) {
                return@addSource
            }
            value = HttpUtils.supportsAuthentication(uri)
        }
    }
}