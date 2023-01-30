package at.bitfire.icsdroid.db.entity

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.icsdroid.db.AppDatabase

/**
 * Stores the credentials to be used with a specific subscription.
 *
 * FIXME Should we allow null username and/or password?
 * @param subscriptionId The id of the subscription that matches this credential.
 * @param username The username of the credential.
 * @param password The password of the credential.
 */
@Entity(
    tableName = "credentials"
)
data class Credential(
    @PrimaryKey val subscriptionId: Long,
    val username: String,
    val password: String,
) {
    constructor(
        subscription: Subscription,
        username: String,
        password: String,
    ) : this(subscription.id, username, password)

    constructor(
        subscription: Subscription,
        username: LiveData<String>,
        password: LiveData<String>,
    ) : this(subscription.id, username.value!!, password.value!!)

    @WorkerThread
    fun put(context: Context) = AppDatabase.getInstance(context)
        .credentialsDao()
        .put(subscriptionId, username, password)
}
