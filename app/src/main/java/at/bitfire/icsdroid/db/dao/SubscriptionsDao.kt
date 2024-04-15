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

@Dao
interface SubscriptionsDao {

    @Insert
    fun add(subscription: Subscription): Long

    @Delete
    fun delete(vararg subscriptions: Subscription)

    @Query("SELECT * FROM subscriptions")
    fun getAllLive(): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id=:id")
    fun getById(id: Long): Subscription?

    @Query("SELECT * FROM subscriptions WHERE calendarId=:calendarId")
    fun getByCalendarId(calendarId: Long?): Subscription?

    @Query("SELECT * FROM subscriptions WHERE url=:url")
    fun getByUrl(url: String): Subscription?

    @Query("SELECT * FROM subscriptions WHERE id=:id")
    fun getWithCredentialsByIdLive(id: Long): LiveData<SubscriptionWithCredential>

    @Query("SELECT errorMessage FROM subscriptions WHERE id=:id")
    fun getErrorMessageLive(id: Long): LiveData<String?>

    @Update
    fun update(subscription: Subscription)

    @Query("UPDATE subscriptions SET calendarId=:calendarId WHERE id=:id")
    fun updateCalendarId(id: Long, calendarId: Long?)

    @Query("UPDATE subscriptions SET lastSync=:lastSync, errorMessage=null WHERE id=:id")
    fun updateStatusNotModified(id: Long, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET eTag=:eTag, lastModified=:lastModified, lastSync=:lastSync, errorMessage=null WHERE id=:id")
    fun updateStatusSuccess(id: Long, eTag: String?, lastModified: Long?, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET errorMessage=:message WHERE id=:id")
    fun updateStatusError(id: Long, message: String?)

    @Query("UPDATE subscriptions SET url=:url WHERE id=:id")
    fun updateUrl(id: Long, url: Uri)


    data class SubscriptionWithCredential(
        @Embedded val subscription: Subscription,
        @Relation(
            parentColumn = "id",
            entityColumn = "subscriptionId"
        )
        val credential: Credential?
    )

}