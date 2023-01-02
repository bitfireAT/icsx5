package at.bitfire.icsdroid.db.entity

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.room.Entity
import androidx.room.Ignore
import at.bitfire.ical4android.*
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.sync.SubscriptionAndroidEvent
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.property.LastModified

@Entity(
    tableName = "events",
    primaryKeys = ["uid", "subscriptionId"]
)
data class SubscriptionEvent(
    val id: Long?,
    val uid: String,
    val subscriptionId: Long,
    val lastModified: Long? = 0L,
) {
    /**
     * Builds a new [SubscriptionEvent] from a [Subscription] and an [Event].
     * @throws NullPointerException If the [event] doesn't have a [Event.uid].
     */
    constructor(subscription: Subscription, event: Event) : this(
        null,
        event.uid!!,
        subscription.id,
        event.lastModified?.dateTime?.time,
    )

    @Ignore
    val lastModifiedObj = lastModified?.let { LastModified(DateTime(it)) }

    /**
     * Gets the [Subscription] that contains this event.
     * @author Arnau Mora
     * @param context The context that is making the request.
     * @return The subscription matching the [subscriptionId].
     * @throws IllegalArgumentException If the stored [subscriptionId] doesn't match a valid subscription.
     */
    @WorkerThread
    suspend fun subscription(context: Context) = AppDatabase.getInstance(context)
        .subscriptionsDao()
        .getById(subscriptionId) ?: throw IllegalArgumentException("The event's parent subscription id ($subscriptionId) doesn't match a valid subscription.")

    /**
     * Converts the current event into a [SubscriptionAndroidEvent]. Fetches an event from the system's calendar that has [subscriptionId] as parent, and [uid]
     * as uid.
     * @author Arnau Mora
     * @param context The context that is making the request.
     * @return A [SubscriptionAndroidEvent] that matches the current event data.
     * @throws IllegalArgumentException If the stored [subscriptionId] doesn't match a valid subscription.
     * @throws NullPointerException If [id] is null.
     */
    @WorkerThread
    suspend fun event(context: Context): SubscriptionAndroidEvent? = subscription(context).queryAndroidEventByUid(context, uid).firstOrNull()
}
