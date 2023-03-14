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
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.TitleColorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.joda.time.Minutes
import org.joda.time.format.PeriodFormat

class TitleColorFragment : Fragment() {

    private val model by activityViewModels<TitleColorModel>()

    private lateinit var binding: TitleColorBinding

    private val checkboxCheckedChanged: OnCheckedChangeListener = OnCheckedChangeListener { _, checked ->
        if (!checked) {
            model.defaultAlarmMinutes.postValue(null)
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
                    model.defaultAlarmMinutes.postValue(editText.text?.toString()?.toLongOrNull())
                    dialog.dismiss()
                }
            }
            .setOnCancelListener {
                binding.defaultAlarmSwitch.isChecked = false
            }
            .create()
            .show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        binding = TitleColorBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = model

        model.defaultAlarmMinutes.observe(viewLifecycleOwner) { min: Long? ->
            binding.defaultAlarmSwitch.isChecked = min != null
            // We add the listener once the switch has an initial value
            binding.defaultAlarmSwitch.setOnCheckedChangeListener(checkboxCheckedChanged)

            if (min == null) {
                binding.defaultAlarmText.visibility = View.GONE
            } else {
                val alarmPeriodText = PeriodFormat.wordBased().print(Minutes.minutes(min.toInt()))
                binding.defaultAlarmText.text = getString(R.string.add_calendar_alarms_default_description, alarmPeriodText)
                binding.defaultAlarmText.visibility = View.VISIBLE
            }
        }

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

        fun dirty(): Boolean = originalTitle != title.value || originalColor != color.value || originalIgnoreAlerts != ignoreAlerts.value ||
                originalDefaultAlarmMinutes != defaultAlarmMinutes.value
    }

}
