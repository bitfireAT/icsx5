/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.ui

import android.content.Context
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.annotation.IntDef
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.databinding.AlertRowBinding
import at.bitfire.icsdroid.databinding.TitleColorBinding

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

        // Initialize the adapter for the reminders list
        val remindersAdapter = RemindersListAdapter(requireContext(), model)
        binding.alertsList.adapter = remindersAdapter

        // When the new reminder button is tapped, create a new blank one
        binding.newAlertButton.setOnClickListener {
            val newList = (model.reminders.value ?: emptyList())
                .toMutableList()
                .apply { add(CalendarReminder.DEFAULT) }
            model.reminders.postValue(newList)
        }

        // When the reminders are updated, submit the new list to the recycler view adapter
        model.reminders.observe(viewLifecycleOwner) { remindersAdapter.submitList(it) }

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

        var originalReminders: List<CalendarReminder>? = null
        val reminders = MutableLiveData<List<CalendarReminder>>()

        var allowedReminders = MutableLiveData<List<Int>>()

        fun dirty() =
            originalTitle != title.value || originalColor != color.value || originalIgnoreAlerts != ignoreAlerts.value || originalReminders != reminders.value
    }

    /**
     * Stores all the reminders registered for a given calendar.
     * @since 20221201
     */
    data class CalendarReminder(
        /**
         * How much time to notify before the event. The unit of this value is determined by [method].
         * @since 20221201
         */
        val time: Long,
        /**
         * The unit to be used with [time]. The possible values are:
         * - `0`: minutes (x1)
         * - `1`: hours (x60)
         * - `2`: days (x1440)
         *
         * This is an index, that also match the value at [R.array.add_calendar_alerts_item_units], this way the selection in the spinner is easier.
         * @since 20221201
         * @see minutes
         */
        @androidx.annotation.IntRange(from = 0, to = 2)
        val units: Int,
        @Method
        val method: Int,
    ) {
        companion object {
            val DEFAULT: CalendarReminder
                get() = CalendarReminder(15, 0, CalendarContract.Reminders.METHOD_DEFAULT)
        }

        @IntDef(
            CalendarContract.Reminders.METHOD_DEFAULT,
            CalendarContract.Reminders.METHOD_ALERT,
            CalendarContract.Reminders.METHOD_EMAIL,
            CalendarContract.Reminders.METHOD_SMS,
            CalendarContract.Reminders.METHOD_ALARM,
        )
        annotation class Method

        /**
         * Provides the [time] specified, adjusted to match the amount of minutes.
         * @since 20221202
         */
        val minutes: Long
            get() = when (units) {
                0 -> time * 1
                1 -> time * 60
                else -> time * 1440
            }
    }

    class RemindersListAdapter(private val context: Context, private val model: TitleColorModel) :
        ListAdapter<CalendarReminder, RemindersListAdapter.ViewHolder>(object : DiffUtil.ItemCallback<CalendarReminder>() {
            override fun areItemsTheSame(oldItem: CalendarReminder, newItem: CalendarReminder): Boolean =
                oldItem.time == newItem.time && oldItem.units == newItem.units && oldItem.method == newItem.method

            override fun areContentsTheSame(oldItem: CalendarReminder, newItem: CalendarReminder): Boolean =
                oldItem.time == newItem.time && oldItem.units == newItem.units && oldItem.method == newItem.method
        }) {
        private val layoutInflater = LayoutInflater.from(context)

        class ViewHolder(val binding: AlertRowBinding) : LifecycleViewHolder<AlertRowBinding>(binding)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            Log.i(Constants.TAG, "Creating view holder")
            val binding = AlertRowBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val reminder = currentList[position]
            val binding = holder.binding

            ArrayAdapter.createFromResource(context, R.array.add_calendar_alerts_item_units, android.R.layout.simple_spinner_item).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.unitsSpinner.adapter = adapter
            }
            model.allowedReminders.observe(holder.lifecycleOwner!!) { allowedReminders ->
                val methods = context.resources.getStringArray(R.array.add_calendar_alerts_item_methods)
                val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, allowedReminders.map { methods[it] })
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.methodSpinner.adapter = adapter
            }

            binding.delete.setOnClickListener {
                // Remove the item at the current position
                val newList = currentList.toMutableList().apply { removeAt(position) }
                model.reminders.postValue(newList)
            }

            // Update the currently selected values
            binding.time.setText(reminder.minutes.toString())
            binding.unitsSpinner.setSelection(reminder.units)
            binding.methodSpinner.setSelection(reminder.method)

            // Add validation listeners
            binding.time.addTextChangedListener { text ->
                // Make sure the value entered is an int
                val number = text?.toString()?.toIntOrNull()
                binding.time.error = if (number == null)
                    context.getString(R.string.add_calendar_alerts_error_number)
                else if (number < 15)
                    context.getString(R.string.add_calendar_alerts_error_min)
                else
                    null
            }
            // TODO: Make sure that the selected method is supported
            binding.methodSpinner.onItemSelectedListener = object : OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) { /* Ignore */
                }

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

                }
            }
        }
    }

}
