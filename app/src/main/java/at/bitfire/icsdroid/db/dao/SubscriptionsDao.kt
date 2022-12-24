package at.bitfire.icsdroid.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import at.bitfire.icsdroid.db.entity.Subscription

@Dao
interface SubscriptionsDao {
    @Query("SELECT * FROM subscriptions")
    fun getAllLive(): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    suspend fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id=:id LIMIT 1")
    suspend fun getById(id: Long): Subscription?

    @Update
    suspend fun update(vararg subscriptions: Subscription)

    @Query("UPDATE subscriptions SET lastSync=:lastSync")
    suspend fun updateStatusNotModified(id: Long, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET eTag=:eTag AND lastModified=:lastModified AND lastSync=:lastSync AND errorMessage=null WHERE id=:id")
    suspend fun updateStatusSuccess(id: Long, eTag: String?, lastModified: Long, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET errorMessage=:message WHERE id=:id")
    suspend fun updateStatusError(id: Long, message: String)
}
