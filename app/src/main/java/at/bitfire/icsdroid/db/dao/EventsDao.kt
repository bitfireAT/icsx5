package at.bitfire.icsdroid.db.dao

import android.database.SQLException
import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.db.entity.SubscriptionEvent

@Dao
interface EventsDao {
    /**
     * Adds one or more new events to the database.
     *
     * **This doesn't add the event to the system's calendar.**
     * @author Arnau Mora
     * @since 20221227
     * @param events All the events to be added.
     * @throws SQLException If any error occurs with the request.
     */
    @Insert
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun add(vararg events: SubscriptionEvent)

    /**
     * Gets a list of all the available events.
     * @author Arnau Mora
     * @since 20221224
     * @return A list of all the events stored in the database.
     */
    @WorkerThread
    @Query("SELECT * FROM events")
    @Throws(SQLException::class)
    suspend fun getEvents(): List<SubscriptionEvent>

    /**
     * Queries a [SubscriptionEvent] from its [SubscriptionEvent.uid].
     * @author Arnau Mora
     * @since 20221224
     * @param subscriptionId The id of the subscription that the event belongs to.
     * @param uid The uid of the event.
     * @return `null` if the event was not found, otherwise, the event requested is returned.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Query("SELECT * FROM events WHERE uid=:uid AND subscriptionId=:subscriptionId LIMIT 1")
    @Throws(SQLException::class)
    suspend fun getEventByUid(subscriptionId: Long, uid: String): SubscriptionEvent?

    /**
     * Removes all the events from the given subscription that are not in the [uids] list provided.
     * @author Arnau Mora
     * @since 20221227
     * @param subscriptionId The id of the parent subscription.
     * @param uids The list of uids to retain.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Query("DELETE FROM events WHERE subscriptionId=:subscriptionId AND uid NOT IN (:uids)")
    @Throws(SQLException::class)
    suspend fun retainByUidFromSubscription(subscriptionId: Long, uids: Set<String>)

    /**
     * Updates the [SubscriptionEvent.id] from its [Subscription.id] and [SubscriptionEvent.uid].
     * @author Arnau Mora
     * @since 20221227
     * @param subscriptionId The id of the event's parent subscription.
     * @param uid The uid of the event to be updated.
     * @param id The new id to set to the event.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Query("UPDATE events SET id=:id WHERE subscriptionId=:subscriptionId AND uid=:uid")
    @Throws(SQLException::class)
    suspend fun updateId(subscriptionId: Long, uid: String, id: Long?)
}