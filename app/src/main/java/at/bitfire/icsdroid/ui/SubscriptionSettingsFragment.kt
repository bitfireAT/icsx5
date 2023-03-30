/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.SubscriptionSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import org.joda.time.Minutes
import org.joda.time.format.PeriodFormat

class SubscriptionSettingsFragment : Fragment() {

    private val model by activityViewModels<TitleColorModel>()

    private lateinit var binding: SubscriptionSettingsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        binding = SubscriptionSettingsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = model

        model.defaultAlarmMinutes.observe(
            viewLifecycleOwner,
            defaultAlarmObserver(
                binding.defaultAlarmSwitch,
                binding.defaultAlarmText,
                model.defaultAlarmMinutes
            )
        )
        model.defaultAllDayAlarmMinutes.observe(
            viewLifecycleOwner,
            defaultAlarmObserver(
                binding.defaultAlarmAllDaySwitch,
                binding.defaultAlarmAllDayText,
                model.defaultAllDayAlarmMinutes
            )
        )

        val colorPickerContract = registerForActivityResult(ColorPickerActivity.Contract()) { color ->
            model.color.postValue(color)
        }
        binding.color.setOnClickListener {
            colorPickerContract.launch(model.color.value)
        }

        return binding.root
    }

    /**
     * Provides an [OnCheckedChangeListener] for watching the checked changes of a switch that
     * provides the alarm time in minutes for a given parameter. Also holds the alert dialog that
     * asks the user the amount of time to set.
     * @param switch The switch that is going to update the selection of minutes.
     * @param observable The state holder of the amount of minutes selected.
     */
    private fun getOnCheckedChangeListener(
        switch: SwitchMaterial,
        observable: MutableLiveData<Long>
    ) = OnCheckedChangeListener { _, checked ->
        if (!checked) {
            observable.postValue(null)
            return@OnCheckedChangeListener
        }

        val editText = EditText(requireContext()).apply {
            setHint(R.string.default_alarm_dialog_hint)
            isSingleLine = true
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_NUMBER

            addTextChangedListener { txt ->
                val text = txt?.toString()
                val num = text?.toLongOrNull()
                error = if (text == null || text.isBlank() || num == null)
                    getString(R.string.default_alarm_dialog_error)
                else
                    null
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.default_alarm_dialog_title)
            .setMessage(R.string.default_alarm_dialog_message)
            .setView(editText)
            .setPositiveButton(R.string.default_alarm_dialog_set) { dialog, _ ->
                if (editText.error == null) {
                    observable.postValue(editText.text?.toString()?.toLongOrNull())
                    dialog.dismiss()
                }
            }
            .setOnCancelListener {
                switch.isChecked = false
            }
            .create()
            .show()
    }

    /**
     * Provides an observer for the default alarm fields.
     * @param switch The switch view that updates the currently stored minutes.
     * @param textView The viewer for the current value of the stored minutes.
     * @param observable The LiveData instance that holds the currently selected amount of minutes.
     */
    private fun defaultAlarmObserver(
        switch: SwitchMaterial,
        textView: TextView,
        observable: MutableLiveData<Long>
    ) = Observer { min: Long? ->
        switch.isChecked = min != null
        // We add the listener once the switch has an initial value
        switch.setOnCheckedChangeListener(getOnCheckedChangeListener(switch, observable))

        if (min == null) {
            textView.visibility = View.GONE
        } else {
            val alarmPeriodText = PeriodFormat.wordBased().print(Minutes.minutes(min.toInt()))
            textView.text = getString(R.string.add_calendar_alarms_default_description, alarmPeriodText)
            textView.visibility = View.VISIBLE
        }
    }

    class TitleColorModel : ViewModel() {
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

}
