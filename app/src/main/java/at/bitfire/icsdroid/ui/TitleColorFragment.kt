/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.TitleColorBinding
import at.bitfire.icsdroid.db.CalendarReminder

class TitleColorFragment : Fragment() {

    private val model by activityViewModels<TitleColorModel>()

    private val colorPickerContract = registerForActivityResult(ColorPickerActivity.Contract()) { color ->
        model.color.postValue(color)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
        val binding = TitleColorBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = model

        // Listener for launching the color picker
        binding.color.setOnClickListener { colorPickerContract.launch(model.color.value) }

        binding.customAlertEnable.setOnCheckedChangeListener { _, checked ->
            if (!checked)
                model.reminder.postValue(null)
            else
                model.reminder.postValue(model.originalReminder ?: CalendarReminder.DEFAULT)
        }
        binding.customAlertTime.addTextChangedListener { text ->
            // Make sure the value entered is an int
            val number = text?.toString()?.toLongOrNull()
            binding.customAlertTime.error = when (number) {
                null -> getString(R.string.add_calendar_alerts_error_number)
                else -> null
            }
            number?.let {
                val reminder = model.reminder.value ?: CalendarReminder.DEFAULT
                val newReminder = reminder.copy(minutes = it)
                // Only update model if the reminder has been changed
                if (reminder != newReminder)
                    model.reminder.postValue(newReminder)
            }
        }
        binding.customAlertMethod.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(adapter: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val reminder = model.reminder.value ?: CalendarReminder.DEFAULT
                model.allowedReminders.value?.let {
                    model.reminder.postValue(reminder.copy(method = it[position]))
                }
            }

            override fun onNothingSelected(adapter: AdapterView<*>?) {}
        }

        model.allowedReminders.observe(viewLifecycleOwner) { allowedReminders ->
            val methods = resources.getStringArray(R.array.add_calendar_alerts_custom_methods)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, allowedReminders.map { methods[it] })
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.customAlertMethod.adapter = adapter
        }

        model.reminder.observe(viewLifecycleOwner) { reminder ->
            if (reminder == null) {
                binding.customAlertCard.visibility = View.GONE
                binding.customAlertEnable.isChecked = false
            } else {
                val minutes = reminder.minutes
                binding.customAlertCard.visibility = View.VISIBLE
                binding.customAlertEnable.isChecked = true
                binding.customAlertTime
                    .takeIf { it.text.toString() != minutes.toString() }
                    ?.setText(minutes.toString())
                binding.customAlertMethod.setSelection(reminder.method)
            }
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

        var originalReminder: CalendarReminder? = null
        val reminder = MutableLiveData<CalendarReminder>()

        var allowedReminders = MutableLiveData<List<Int>>()

        fun dirty() = arrayOf(
            originalTitle to title,
            originalColor to color,
            originalIgnoreAlerts to ignoreAlerts,
            originalReminder to reminder,
        ).any { (original, state) -> original != state.value }
    }

}
