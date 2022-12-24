package at.bitfire.icsdroid.db.dao

import android.database.SQLException
import androidx.annotation.WorkerThread
import androidx.room.Query
import at.bitfire.icsdroid.db.entity.SubscriptionEvent

interface EventsDao {
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
}