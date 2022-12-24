package at.bitfire.icsdroid.db.entity

import android.content.Context
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.AndroidEvent

@Entity(tableName = "events")
data class SubscriptionEvent(
    @PrimaryKey
    var uid: String? = null,
    @PrimaryKey
    var subscriptionId: Long,
    var lastModified: Long? = 0L,
) {
    constructor(subscription: Subscription, event: at.bitfire.ical4android.Event): this(
        event.uid,
        subscription.id,
        event.lastModified?.dateTime?.time,
    )

    /**
     * Adds the event to the system's calendar storage.
     * @author Arnau Mora
     * @since 20221224
     */
    fun add(context: Context) {
        TODO()
    }
}
