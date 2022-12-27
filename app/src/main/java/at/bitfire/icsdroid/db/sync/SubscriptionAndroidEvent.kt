package at.bitfire.icsdroid.db.sync

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.core.content.contentValuesOf
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidEvent
import at.bitfire.ical4android.AndroidEventFactory
import at.bitfire.ical4android.Event
import at.bitfire.icsdroid.db.entity.Subscription

class SubscriptionAndroidEvent: AndroidEvent {
    var uid: String? = null
    var lastModified = 0L

    constructor(calendar: AndroidCalendar<AndroidEvent>, contentValues: ContentValues): super(calendar, contentValues)

    constructor(calendar: AndroidCalendar<AndroidEvent>, event: Event): super(calendar, event)

    constructor(context: Context, subscription: Subscription, event: Event): this(subscription.getCalendar(context), event)

    constructor(context: Context, subscription: Subscription, id: Long): this(subscription.getCalendar(context), id)

    @Deprecated("Try passing contentValues directly.")
    constructor(calendar: AndroidCalendar<AndroidEvent>, id: Long): this(
        calendar,
        contentValuesOf(
            CalendarContract.Events._ID to id,
        ),
    )

    class Factory: AndroidEventFactory<SubscriptionAndroidEvent> {
        override fun fromProvider(calendar: AndroidCalendar<AndroidEvent>, values: ContentValues): SubscriptionAndroidEvent =
            SubscriptionAndroidEvent(calendar, values)
    }
}
