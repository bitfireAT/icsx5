package at.bitfire.icsdroid.ui.subscription

import android.content.Intent
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.ui.AddCalendarActivity.Companion.EXTRA_COLOR
import at.bitfire.icsdroid.ui.AddCalendarActivity.Companion.EXTRA_TITLE

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

    val isDirty = MediatorLiveData<Boolean>().apply {
        addSource(title) { value = dirty() }
        addSource(color) { value = dirty() }
        addSource(ignoreAlerts) { value = dirty() }
        addSource(defaultAlarmMinutes) { value = dirty() }
        addSource(defaultAllDayAlarmMinutes) { value = dirty() }
    }

    private fun dirty(): Boolean =
        originalTitle != title.value ||
            originalColor != color.value ||
            originalIgnoreAlerts != ignoreAlerts.value ||
            originalDefaultAlarmMinutes != defaultAlarmMinutes.value ||
            originalDefaultAllDayAlarmMinutes != defaultAllDayAlarmMinutes.value

    /**
     * Uses the provided [intent] to fill the states of some live data. Associations:
     * - [Intent.getData] :: [url]
     * - [EXTRA_TITLE] :: [title]
     * - [EXTRA_COLOR] :: [color]
     *
     * Any of them can be null.
     */
    fun loadFromIntent(intent: Intent) = intent.apply {
        if (data != null) {
            url.postValue(data.toString())
        }
        if (hasExtra(EXTRA_TITLE)) {
            title.postValue(getStringExtra(EXTRA_TITLE))
        }
        if (hasExtra(EXTRA_COLOR)) {
            color.postValue(getIntExtra(EXTRA_COLOR, LocalCalendar.DEFAULT_COLOR))
        }
    }
}
