package at.bitfire.icsdroid.db.entity

import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Stores the credentials to be used with a specific subscription.
 * @param subscriptionId The id of the subscription that matches this credential.
 * @param username The username of the credential.
 * @param password The password of the credential.
 */
@Entity(
    tableName = "credentials",
    foreignKeys = [
        ForeignKey(entity = Subscription::class, parentColumns = ["id"], childColumns = ["subscriptionId"], onDelete = ForeignKey.CASCADE),
    ],
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

}