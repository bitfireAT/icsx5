package at.bitfire.icsdroid.db.entity

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.icsdroid.db.AppDatabase

@Entity(
    tableName = "credentials"
)
data class Credential(
    @PrimaryKey val subscriptionId: Long,
    val username: String?,
    val password: String?,
) {
    constructor(
        subscription: Subscription,
        username: String?,
        password: String?,
    ) : this(subscription.id, username, password)

    constructor(
        subscription: Subscription,
        username: LiveData<String?>,
        password: LiveData<String?>,
    ) : this(subscription.id, username.value, password.value)

    @WorkerThread
    suspend fun put(context: Context) = AppDatabase.getInstance(context)
        .credentialsDao()
        .put(subscriptionId, username, password)
}
