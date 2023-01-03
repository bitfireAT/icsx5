package at.bitfire.icsdroid.db.sync

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import at.bitfire.ical4android.*
import at.bitfire.icsdroid.db.entity.Subscription
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.LastModified

/**
 * Serves as an interface with the system's calendar.
 *
 * Formerly `LocalEvent`.
 */
class LocalEvent : AndroidEvent {

    companion object {
        /**
         * A custom column for the calendar contract that allows storing the last modification date.
         */
        const val COLUMN_LAST_MODIFIED = CalendarContract.Events.SYNC_DATA2
    }

    var uid: String? = null
    var lastModified = 0L

    /**
     * Used by the [Factory] to create new events from [ContentValues].
     *
     * [contentValues] must have:
     * - [CalendarContract.Events._SYNC_ID]
     * - [COLUMN_LAST_MODIFIED]
     * @param calendar The calendar in which the event is contained.
     * @param contentValues The container of the values to be inserted.
     */
    private constructor(
        calendar: AndroidCalendar<AndroidEvent>,
        contentValues: ContentValues
    ) : super(calendar, contentValues) {
        uid = contentValues.getAsString(CalendarContract.Events._SYNC_ID)
        lastModified = contentValues.getAsLong(COLUMN_LAST_MODIFIED) ?: 0
    }

    /**
     * Creates a new event from an [AndroidCalendar] and an [Event].
     * @param calendar The calendar in which the event is contained.
     * @param event The event to copy the data from. [Event.uid] and [Event.lastModified] is taken.
     */
    constructor(calendar: AndroidCalendar<AndroidEvent>, event: Event) : super(calendar, event) {
        uid = event.uid
        lastModified = event.lastModified?.dateTime?.time ?: 0
    }

    /**
     * Creates a new event from a [Subscription] and an [Event]. Uses [Subscription.getCalendar] to
     * know the calendar in which the [event] is included.
     * @param context The context that is making the request.
     * @param subscription The subscription in which to include the event.
     * @param event The event to copy the data from. [Event.uid] and [Event.lastModified] is taken.
     */
    constructor(
        context: Context,
        subscription: Subscription,
        event: Event
    ) : this(subscription.getCalendar(context), event)

    override fun populateEvent(row: ContentValues, groupScheduled: Boolean) {
        super.populateEvent(row, groupScheduled)

        val event = requireNotNull(event)
        event.uid = row.getAsString(CalendarContract.Events._SYNC_ID)

        row.getAsLong(COLUMN_LAST_MODIFIED).let {
            lastModified = it
            event.lastModified = LastModified(DateTime(it))
        }
    }

    override fun buildEvent(recurrence: Event?, builder: BatchOperation.CpoBuilder) {
        super.buildEvent(recurrence, builder)

        if (recurrence == null) {
            // master event
            builder.withValue(CalendarContract.Events._SYNC_ID, uid)
                .withValue(COLUMN_LAST_MODIFIED, lastModified)
        } else {
            // exception
            builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, uid)
        }
    }

    /**
     * Provides a builder for events to be used with [AndroidCalendar].
     */
    class Factory : AndroidEventFactory<LocalEvent> {
        override fun fromProvider(
            calendar: AndroidCalendar<AndroidEvent>,
            values: ContentValues
        ): LocalEvent = LocalEvent(calendar, values)
    }
}
