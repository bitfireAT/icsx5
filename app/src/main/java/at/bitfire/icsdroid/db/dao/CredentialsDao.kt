package at.bitfire.icsdroid.db.dao

import android.database.SQLException
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription

@Dao
interface CredentialsDao {
    /**
     * Gets all the credentials stored for the given subscription.
     * @param subscriptionId The id of the subscription to get the credentials for.
     * @return The [Credential] stored for the given [subscriptionId] or null if none.
     * @throws SQLException If there's an error while fetching the credential.
     */
    @Query("SELECT * FROM credentials WHERE subscriptionId=:subscriptionId")
    fun getBySubscriptionId(subscriptionId: Long): Credential?

    /**
     * Inserts a new credential into the table.
     */
    @Insert
    fun create(credential: Credential)

    /**
     * Removes the credentials stored for the given subscription from the database.
     * @param subscriptionId The id ([Subscription.id]) of the subscription that matches the stored
     * credentials to be deleted.
     * @throws SQLException If there's an error while making the deletion.
     */
    @Query("DELETE FROM credentials WHERE subscriptionId=:subscriptionId")
    fun remove(subscriptionId: Long)

}