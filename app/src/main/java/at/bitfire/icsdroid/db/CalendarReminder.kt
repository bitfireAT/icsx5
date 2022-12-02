package at.bitfire.icsdroid.db

import android.provider.CalendarContract
import androidx.annotation.IntDef
import at.bitfire.icsdroid.R

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

        /**
         * Converts back into a [CalendarReminder] the contents that have been serialized with [CalendarReminder.serialize].
         * @author Arnau Mora
         * @since 20221202
         * @param string The text to convert.
         * @return An initialized instance of [CalendarReminder] with the data provided by [string].
         * @throws IllegalArgumentException When the given [string] is not valid. Usually because the length of the parameters is not correct, or because one or
         * more parameters could not be converted back to Long/Int.
         * @see serialize
         */
        @Throws(IllegalArgumentException::class)
        fun parse(string: String): CalendarReminder = string.split(",").let { pieces ->
            if (pieces.size != 3)
                throw IllegalArgumentException("The provided string is not valid ({Long},{Int},{Int}): $string")
            val time = pieces[0].toLongOrNull()
            val units = pieces[1].toIntOrNull()
            val method = pieces[2].toIntOrNull()
            if (time == null || units == null || method == null)
                throw IllegalArgumentException("The provided string is not valid ({Long},{Int},{Int}): $string")

            CalendarReminder(time, units, method)
        }
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

    /**
     * Converts the data in the class into a [String] that then can be converted back again into a [CalendarReminder].
     * @author Arnau Mora
     * @since 20221202
     * @return The fields of the class turned into a [String].
     * @see parse
     */
    fun serialize(): String = arrayOf(time, units, method).joinToString(",")
}

/**
 * Maps each element of the collection into a [String] with [CalendarReminder.serialize] and joins all the resulting elements with `;`.
 * @author Arnau Mora
 * @since 20221202
 */
fun Iterable<CalendarReminder>.serialize() = joinToString(";") { it.serialize() }
