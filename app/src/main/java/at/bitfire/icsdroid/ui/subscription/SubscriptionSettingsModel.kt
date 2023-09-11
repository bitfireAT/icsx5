package at.bitfire.icsdroid.ui.subscription

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SubscriptionSettingsModel : ViewModel() {
    var url = MutableLiveData<String>()

    var originalTitle: String? = null
    val title = MutableLiveData<String>()

    var originalColor: Int? = null
    val color = MutableLiveData<Int>()

    var originalIgnoreAlerts: Boolean? = null
    val ignoreAlerts = MutableLiveData<Boolean>()

    var originalDefaultAlarmMinutes: Long? = null
    val defaultAlarmMinutes = MutableLiveData<Long>()

    var originalDefaultAllDayAlarmMinutes: Long? = null
    val defaultAllDayAlarmMinutes = MutableLiveData<Long>()

    fun dirty(): Boolean = originalTitle != title.value || originalColor != color.value || originalIgnoreAlerts != ignoreAlerts.value ||
            originalDefaultAlarmMinutes != defaultAlarmMinutes.value || originalDefaultAllDayAlarmMinutes != defaultAllDayAlarmMinutes.value
}
