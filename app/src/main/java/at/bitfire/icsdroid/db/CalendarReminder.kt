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
     * How many minutes to alert before the event.
     * @since 20221201
     */
    val minutes: Long,
    @Method
    val method: Int,
) {
    companion object {
        val DEFAULT: CalendarReminder
            get() = CalendarReminder(15, CalendarContract.Reminders.METHOD_DEFAULT)

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
            if (pieces.size != 2)
                throw IllegalArgumentException("The provided string is not valid ({Long},{Int}): $string")
            val time = pieces[0].toLongOrNull()
            val method = pieces[1].toIntOrNull()
            if (time == null || method == null)
                throw IllegalArgumentException("The provided string is not valid ({Long},{Int}): $string")

            CalendarReminder(time, method)
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
     * Converts the data in the class into a [String] that then can be converted back again into a [CalendarReminder].
     * @author Arnau Mora
     * @since 20221202
     * @return The fields of the class turned into a [String].
     * @see parse
     */
    fun serialize(): String = arrayOf(minutes, method).joinToString(",")
}
