package at.bitfire.icsdroid.db.dao

import androidx.room.*
import at.bitfire.icsdroid.db.entity.Credential

@Dao
interface CredentialsDao {

    @Query("SELECT * FROM credentials WHERE subscriptionId=:subscriptionId")
    fun getBySubscriptionId(subscriptionId: Long): Credential?

    @Insert
    fun create(credential: Credential)

    @Upsert
    fun upsert(credential: Credential)

    @Query("DELETE FROM credentials WHERE subscriptionId=:subscriptionId")
    fun removeBySubscriptionId(subscriptionId: Long)

    /** Updates a given calendar in the table */
    @Update
    fun update(credential: Credential)

}