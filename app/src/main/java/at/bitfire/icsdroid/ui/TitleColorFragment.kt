/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.TitleColorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.roundToInt

const val MINUTES_IN_AN_HOUR = 60f
const val MINUTES_IN_A_DAY = 24*60f
const val MINUTES_IN_AN_WEEK = 7*24*60f
const val MINUTES_IN_A_MONTH = 30*7*24*60f

class TitleColorFragment : Fragment() {

    private val model by activityViewModels<TitleColorModel>()

    private val colorPickerContract = registerForActivityResult(ColorPickerActivity.Contract()) { color ->
        model.color.postValue(color)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val binding = TitleColorBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = model

        var firstCheck = true

        model.defaultAlarmMinutes.observe(viewLifecycleOwner) { min: Long? ->
            binding.defaultAlarmSwitch.isChecked = min != null
            firstCheck = false
            if (min == null) {
                binding.defaultAlarmText.visibility = View.GONE
                return@observe
            }
            // TODO: Build string to have exactly the duration specified. e.g.: 1 hour and 37 minutes
            val minutes = min.toInt()
            val text = if (minutes < MINUTES_IN_AN_HOUR)
                resources.getQuantityString(R.plurals.add_calendar_alarms_custom_minutes, minutes, minutes)
            else if (minutes < MINUTES_IN_A_DAY) (minutes / MINUTES_IN_AN_HOUR).roundToInt().let {
                resources.getQuantityString(R.plurals.add_calendar_alarms_custom_hours, it, it)
            }
            else if (minutes < MINUTES_IN_AN_WEEK) (minutes / MINUTES_IN_A_DAY).roundToInt().let {
                resources.getQuantityString(R.plurals.add_calendar_alarms_custom_days, it, it)
            }
            else if (minutes < MINUTES_IN_A_MONTH) (minutes / MINUTES_IN_AN_WEEK).roundToInt().let {
                resources.getQuantityString(R.plurals.add_calendar_alarms_custom_weeks, it, it)
            }
            else (minutes / MINUTES_IN_A_MONTH).roundToInt().let {
                resources.getQuantityString(R.plurals.add_calendar_alarms_custom_months, it, it)
            }
            binding.defaultAlarmText.text = getString(R.string.add_calendar_alarms_default_description, text)
            binding.defaultAlarmText.visibility = View.VISIBLE
        }

        // Listener for launching the color picker
        binding.color.setOnClickListener { colorPickerContract.launch(model.color.value) }

        binding.defaultAlarmSwitch.setOnCheckedChangeListener { _, checked ->
            if (firstCheck) return@setOnCheckedChangeListener

            if (!checked) {
                model.defaultAlarmMinutes.postValue(null)
                return@setOnCheckedChangeListener
            }

            val editText = EditText(requireContext()).apply {
                setHint(R.string.default_alarm_dialog_hint)

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
                    if (editText.error != null) {
                        // TODO: Value introduced is not valid
                    } else {
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

        fun dirty() = arrayOf(
            originalTitle to title,
            originalColor to color,
            originalIgnoreAlerts to ignoreAlerts,
            originalDefaultAlarmMinutes to defaultAlarmMinutes,
        ).any { (original, state) -> original != state.value }
    }

}
