package at.bitfire.icsdroid.db.entity

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.ical4android.*
import at.bitfire.icsdroid.db.AppDatabase

@Entity(
    tableName = "events",
    primaryKeys = [ "uid", "subscriptionId"]
)
data class SubscriptionEvent(
    val uid: String,
    val subscriptionId: Long,
    val lastModified: Long? = 0L,
) {
    /**
     * Builds a new [SubscriptionEvent] from a [Subscription] and an [Event].
     * @since 20221226
     * @throws NullPointerException If the [event] doesn't have a [Event.uid].
     */
    @Throws(NullPointerException::class)
    constructor(subscription: Subscription, event: Event): this(
        event.uid!!,
        subscription.id,
        event.lastModified?.dateTime?.time,
    )

    /**
     * Gets the [Subscription] that contains this event.
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @return The subscription matching the [subscriptionId].
     * @throws IllegalArgumentException If the stored [subscriptionId] doesn't match a valid subscription.
     */
    @WorkerThread
    @Throws(IllegalArgumentException::class)
    suspend fun subscription(context: Context) = AppDatabase.getInstance(context)
        .subscriptionsDao()
        .getById(subscriptionId) ?: throw IllegalArgumentException("The event's parent subscription id ($subscriptionId) doesn't match a valid subscription.")

    /**
     * Converts the current event into an [AndroidEvent].
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @return An [AndroidEvent] that matches the current event data.
     * @throws IllegalArgumentException If the stored [subscriptionId] doesn't match a valid subscription.
     */
    @WorkerThread
    suspend fun event(context: Context): AndroidEvent {
        val calendar = subscription(context).getCalendar(context)
        return object : AndroidEvent(calendar) {
            override fun populateEvent(row: ContentValues, groupScheduled: Boolean) {
                super.populateEvent(row, groupScheduled)
                // TODO: Fill with class data
            }
        }
    }
}
