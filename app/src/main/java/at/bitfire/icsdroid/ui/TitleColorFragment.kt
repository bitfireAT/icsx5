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
import at.bitfire.icsdroid.databinding.TitleColorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import org.joda.time.Minutes
import org.joda.time.format.PeriodFormat

class TitleColorFragment : Fragment() {

    private val model by activityViewModels<TitleColorModel>()

    private lateinit var binding: TitleColorBinding

    /**
     * Provides an observer for the default alarm fields.
     * @param switch The switch view that updates the currently stored minutes.
     * @param textView The viewer for the current value of the stored minutes.
     * @param onValueChanged Gets called when the value of the stored minutes must be updated.
     * Usually contains a call to a ViewModel to update the stored state.
     */
    private fun defaultAlarmObserver(
        switch: SwitchMaterial,
        textView: TextView,
        onValueChanged: (Long?) -> Unit,
    ): Observer<Long> {
        val checkboxCheckedChanged = OnCheckedChangeListener { _, checked ->
            if (!checked) {
                onValueChanged(null)
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
                        onValueChanged(editText.text?.toString()?.toLongOrNull())
                        dialog.dismiss()
                    }
                }
                .setOnCancelListener {
                    switch.isChecked = false
                }
                .create()
                .show()
        }

        return Observer<Long> { min: Long? ->
            switch.isChecked = min != null
            // We add the listener once the switch has an initial value
            switch.setOnCheckedChangeListener(checkboxCheckedChanged)

            if (min == null) {
                textView.visibility = View.GONE
            } else {
                val alarmPeriodText = PeriodFormat.wordBased().print(Minutes.minutes(min.toInt()))
                textView.text = getString(R.string.add_calendar_alarms_default_description, alarmPeriodText)
                textView.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        binding = TitleColorBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = model

        model.defaultAlarmMinutes.observe(
            viewLifecycleOwner,
            defaultAlarmObserver(binding.defaultAlarmSwitch, binding.defaultAlarmText) {
                model.defaultAlarmMinutes.postValue(it)
            }
        )
        model.defaultAllDayAlarmMinutes.observe(
            viewLifecycleOwner,
            defaultAlarmObserver(binding.defaultAlarmAllDaySwitch, binding.defaultAlarmAllDayText) {
                model.defaultAllDayAlarmMinutes.postValue(it)
            }
        )

        val colorPickerContract = registerForActivityResult(ColorPickerActivity.Contract()) { color ->
            model.color.postValue(color)
        }
        binding.color.setOnClickListener {
            colorPickerContract.launch(model.color.value)
        }

        return binding.root
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
