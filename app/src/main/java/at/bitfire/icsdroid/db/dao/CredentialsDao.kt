package at.bitfire.icsdroid.db.dao

import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.WorkerThread
import androidx.room.Dao
import androidx.room.Query
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription

/**
 * Creates an interface with all the credentials stored in the database.
 * @see AppDatabase
 * @see Credential
 */
@Dao
interface CredentialsDao {
    companion object {
        /**
         * Alias for [AppDatabase.getInstance] -> [AppDatabase.credentialsDao].
         * @param context The context that is requesting access to the dao.
         */
        fun getInstance(context: Context) = AppDatabase.getInstance(context).credentialsDao()
    }

    /**
     * Gets all the credentials stored for the given subscription.
     * @param subscriptionId The id of the subscription to get the credentials for.
     * @return The [Credential] stored for the given [subscriptionId] or null if none.
     * @throws SQLException If there's an error while fetching the credential.
     */
    @WorkerThread
    @Query("SELECT * FROM credentials WHERE subscriptionId=:subscriptionId")
    fun get(subscriptionId: Long): Credential?

    /**
     * Inserts a new credential into the table.
     * @param subscriptionId The id ([Subscription.id]) of the parent subscription of the credential.
     * @param username The username to use for the credential.
     * @param password The password to use for the credential.
     * @throws SQLException If there's an error while making the insert.
     * @throws SQLiteConstraintException If there's already a credential for the given subscription.
     */
    @WorkerThread
    @Query("INSERT INTO credentials (subscriptionId, username, password) VALUES (:subscriptionId, :username, :password)")
    fun put(subscriptionId: Long, username: String?, password: String?)

    /**
     * Removes the credentials stored for the given subscription from the database.
     * @param subscriptionId The id ([Subscription.id]) of the subscription that matches the stored
     * credentials to be deleted.
     * @throws SQLException If there's an error while making the deletion.
     */
    @WorkerThread
    @Query("DELETE FROM credentials WHERE subscriptionId=:subscriptionId")
    fun pop(subscriptionId: Long)
}
