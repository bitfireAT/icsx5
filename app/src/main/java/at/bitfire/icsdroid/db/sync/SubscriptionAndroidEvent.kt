package at.bitfire.icsdroid.db.sync

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import at.bitfire.ical4android.*
import at.bitfire.icsdroid.db.entity.Subscription
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.LastModified

class SubscriptionAndroidEvent : AndroidEvent {
    var uid: String? = null
    var lastModified = 0L

    private constructor(calendar: AndroidCalendar<AndroidEvent>, contentValues: ContentValues) : super(calendar, contentValues) {
        uid = contentValues.getAsString(CalendarContract.Events._SYNC_ID)
    }

    constructor(calendar: AndroidCalendar<AndroidEvent>, event: Event) : super(calendar, event) {
        uid = event.uid
        lastModified = event.lastModified?.dateTime?.time ?: 0
    }

    constructor(context: Context, subscription: Subscription, event: Event) : this(subscription.getCalendar(context), event)

    override fun populateEvent(row: ContentValues, groupScheduled: Boolean) {
        super.populateEvent(row, groupScheduled)

        val event = requireNotNull(event)
        event.uid = uid
        event.lastModified = LastModified(DateTime(lastModified))
    }

    override fun buildEvent(recurrence: Event?, builder: BatchOperation.CpoBuilder) {
        super.buildEvent(
            recurrence,
            if (recurrence == null)
            // master event
                builder.withValue(CalendarContract.Events._SYNC_ID, uid)
            else
            // exception
                builder.withValue(CalendarContract.Events.ORIGINAL_SYNC_ID, uid)
        )
    }

    class Factory : AndroidEventFactory<SubscriptionAndroidEvent> {
        override fun fromProvider(calendar: AndroidCalendar<AndroidEvent>, values: ContentValues): SubscriptionAndroidEvent =
            SubscriptionAndroidEvent(calendar, values)
    }
}
