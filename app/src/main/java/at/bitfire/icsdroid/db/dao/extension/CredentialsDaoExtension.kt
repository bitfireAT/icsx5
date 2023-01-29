package at.bitfire.icsdroid.db.dao.extension

import android.database.SQLException
import android.database.sqlite.SQLiteConstraintException
import androidx.annotation.WorkerThread
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription

/**
 * Gets all the credentials stored for the given subscription.
 * @param subscription The subscription to get the credentials for.
 * @return The [Credential] stored for the given [subscription] or null if none.
 * @throws SQLException If there's an error while fetching the credential.
 */
@WorkerThread
fun CredentialsDao.get(subscription: Subscription): Credential? = get(subscription.id)

/**
 * Inserts a new credential into the table.
 * @param credential The [Credential] to store.
 * @throws SQLException If there's an error while making the insert.
 * @throws SQLiteConstraintException If there's already a credential for the given subscription.
 */
@WorkerThread
fun CredentialsDao.put(credential: Credential) = with(credential) {
    put(subscriptionId, username, password)
}

/**
 * Removes the credentials stored for the given subscription from the database.
 * @param subscription The the subscription that matches the stored credentials to be deleted.
 * @throws SQLException If there's an error while making the deletion.
 */
@WorkerThread
fun CredentialsDao.pop(subscription: Subscription) = pop(subscription.id)
