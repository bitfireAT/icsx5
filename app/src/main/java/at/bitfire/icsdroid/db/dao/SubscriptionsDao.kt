package at.bitfire.icsdroid.db.dao

import android.database.SQLException
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription

/**
 * Creates an interface with all the subscriptions made in the database.
 * @author Arnau Mora
 * @since 20221224
 * @see AppDatabase
 */
@Dao
interface SubscriptionsDao {
    /**
     * Adds one or more new subscriptions to the database.
     *
     * **This doesn't add the subscription to the system's calendar.**
     * @author Arnau Mora
     * @since 20221227
     * @param subscriptions All the subscriptions to be added.
     * @throws SQLException If any error occurs with the request.
     */
    @Insert
    @WorkerThread
    @Throws(SQLException::class)
    suspend fun add(vararg subscriptions: Subscription)

    /**
     * Gets a [LiveData] with all the made subscriptions. Updates automatically when new ones are added.
     * @author Arnau Mora
     * @since 20221224
     * @throws SQLException If any error occurs with the request.
     */
    @Query("SELECT * FROM subscriptions")
    @Throws(SQLException::class)
    fun getAllLive(): LiveData<List<Subscription>>

    /**
     * Gets a list of all the made subscriptions.
     * @author Arnau Mora
     * @since 20221224
     * @throws SQLException If any error occurs with the request.
     */
    @WorkerThread
    @Query("SELECT * FROM subscriptions")
    @Throws(SQLException::class)
    suspend fun getAll(): List<Subscription>

    /**
     * Gets an specific [Subscription] by its id ([Subscription.id]).
     * @author Arnau Mora
     * @since 20221224
     * @param id The id of the subscription to fetch.
     * @return The [Subscription] indicated, or null if any.
     * @throws SQLException If any error occurs with the request.
     */
    @WorkerThread
    @Query("SELECT * FROM subscriptions WHERE id=:id LIMIT 1")
    @Throws(SQLException::class)
    suspend fun getById(id: Long): Subscription?

    /**
     * Gets a [LiveData] that gets updated with the error message of the given subscription.
     * @author Arnau Mora
     * @since 20221224
     * @param id The id of the subscription to get updates from.
     * @throws SQLException If any error occurs with the request.
     */
    @Query("SELECT errorMessage FROM subscriptions WHERE id=:id")
    @Throws(SQLException::class)
    fun getErrorMessageLive(id: Long): LiveData<String?>

    /**
     * Updates the given subscriptions in the database.
     * @author Arnau Mora
     * @since 20221224
     * @param subscriptions All the subscriptions to be updated.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Update
    @Throws(SQLException::class)
    suspend fun update(vararg subscriptions: Subscription)

    /**
     * Updates the status of a subscription that has not been modified. This is updating its [Subscription.lastSync] to the current time.
     * @author Arnau Mora
     * @since 20221224
     * @param id The id of the subscription to update.
     * @param lastSync The synchronization time to set. Can be left as default, and will match the current system time.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Query("UPDATE subscriptions SET lastSync=:lastSync WHERE id=:id")
    @Throws(SQLException::class)
    suspend fun updateStatusNotModified(id: Long, lastSync: Long = System.currentTimeMillis())

    /**
     * Updates the status of a subscription that has just been modified. This removes its [Subscription.errorMessage], and updates the [Subscription.eTag],
     * [Subscription.lastModified] and [Subscription.lastSync].
     * @author Arnau Mora
     * @since 20221224
     * @param id The id of the subscription to update.
     * @param eTag The new eTag to set.
     * @param lastModified The new date to set for [Subscription.lastModified].
     * @param lastSync The last synchronization date to set. Defaults to the current system time, so can be skipped.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Query("UPDATE subscriptions SET eTag=:eTag AND lastModified=:lastModified AND lastSync=:lastSync AND errorMessage=null WHERE id=:id")
    @Throws(SQLException::class)
    suspend fun updateStatusSuccess(id: Long, eTag: String?, lastModified: Long, lastSync: Long = System.currentTimeMillis())

    /**
     * Updates the error message of the subscription.
     * @author Arnau Mora
     * @since 20221224
     * @param id The id of the subscription to update.
     * @param message The error message to give to the subscription.
     * @throws SQLException If any error occurs with the update.
     */
    @WorkerThread
    @Query("UPDATE subscriptions SET errorMessage=:message WHERE id=:id")
    @Throws(SQLException::class)
    suspend fun updateStatusError(id: Long, message: String?)
}
