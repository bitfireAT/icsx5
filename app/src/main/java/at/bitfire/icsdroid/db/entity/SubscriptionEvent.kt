package at.bitfire.icsdroid.db.entity

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.annotation.WorkerThread
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import at.bitfire.ical4android.*
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.LocalEvent
import at.bitfire.icsdroid.db.sync.SubscriptionAndroidEvent
import at.bitfire.icsdroid.ui
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
     * @since 20221226
     * @throws NullPointerException If the [event] doesn't have a [Event.uid].
     */
    @Throws(NullPointerException::class)
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
     * Converts the current event into a [SubscriptionAndroidEvent].
     * @author Arnau Mora
     * @since 20221227
     * @param context The context that is making the request.
     * @return A [SubscriptionAndroidEvent] that matches the current event data.
     * @throws IllegalArgumentException If the stored [subscriptionId] doesn't match a valid subscription.
     * @throws NullPointerException If [id] is null.
     */
    @WorkerThread
    @Throws(IllegalArgumentException::class, NullPointerException::class)
    suspend fun event(context: Context): SubscriptionAndroidEvent? = subscription(context).queryAndroidEventById(context, uid).firstOrNull()
}
