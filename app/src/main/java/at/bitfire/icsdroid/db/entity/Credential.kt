package at.bitfire.icsdroid.db.entity

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.utils.getStringOrNull
import at.bitfire.icsdroid.utils.serialization.JsonSerializable
import at.bitfire.icsdroid.utils.serialization.JsonSerializer
import org.json.JSONObject

@Entity(
    tableName = "credentials"
)
data class Credential(
    @PrimaryKey val subscriptionId: Long,
    val username: String?,
    val password: String?,
): JsonSerializable {
    companion object: JsonSerializer<Credential> {
        /** Converts the given [json] into [Credential]. */
        override fun fromJSON(json: JSONObject): Credential = Credential(
            json.getLong("subscriptionId"),
            json.getStringOrNull("username"),
            json.getStringOrNull("password"),
        )
    }

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

    /** Converts the stored [Credential] into a [JSONObject] */
    override fun toJSON(): JSONObject = JSONObject().apply {
        put("subscriptionId", subscriptionId)
        put("username", username)
        put("password", password)
    }
}
