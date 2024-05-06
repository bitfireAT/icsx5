package at.bitfire.icsdroid.db.dao

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionsDao {

    @Insert
    suspend fun add(subscription: Subscription): Long

    @Delete
    suspend fun delete(vararg subscriptions: Subscription)

    @Query("SELECT * FROM subscriptions")
    fun getAllFlow(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    suspend fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id=:id")
    suspend fun getById(id: Long): Subscription?

    @Query("SELECT * FROM subscriptions WHERE calendarId=:calendarId")
    suspend fun getByCalendarId(calendarId: Long?): Subscription?

    @Query("SELECT * FROM subscriptions WHERE url=:url")
    suspend fun getByUrl(url: String): Subscription?

    @Query("SELECT * FROM subscriptions WHERE id=:id")
    fun getWithCredentialsByIdFlow(id: Long): Flow<SubscriptionWithCredential>

    @Query("SELECT errorMessage FROM subscriptions WHERE id=:id")
    fun getErrorMessageFlow(id: Long): Flow<String?>

    @Update
    suspend fun update(subscription: Subscription)

    @Query("UPDATE subscriptions SET calendarId=:calendarId WHERE id=:id")
    suspend fun updateCalendarId(id: Long, calendarId: Long?)

    @Query("UPDATE subscriptions SET lastSync=:lastSync, errorMessage=null WHERE id=:id")
    suspend fun updateStatusNotModified(id: Long, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET eTag=:eTag, lastModified=:lastModified, lastSync=:lastSync, errorMessage=null WHERE id=:id")
    suspend fun updateStatusSuccess(id: Long, eTag: String?, lastModified: Long?, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET errorMessage=:message WHERE id=:id")
    suspend fun updateStatusError(id: Long, message: String?)

    @Query("UPDATE subscriptions SET url=:url WHERE id=:id")
    suspend fun updateUrl(id: Long, url: Uri)


    data class SubscriptionWithCredential(
        @Embedded val subscription: Subscription,
        @Relation(
            parentColumn = "id",
            entityColumn = "subscriptionId"
        )
        val credential: Credential?
    )

}