package at.bitfire.icsdroid.db

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Context
import android.os.ParcelFileDescriptor
import at.bitfire.icsdroid.utils.toJSONArray
import org.json.JSONArray
import org.json.JSONObject

class Backup: BackupAgent() {
    companion object {
        /**
         * Converts all the data stored in the database into a [JSONObject].
         * @param context The [Context] that is making the request.
         * @return A [JSONObject] with two keys, `subscriptions` and `credentials`. Each of them
         * contains a [JSONArray] that contain all the subscriptions and all the credentials
         * respectively.
         */
        suspend fun createBackup(context: Context): JSONObject {
            val database = AppDatabase.getInstance(context)
            val subscriptionsJson = database
                .subscriptionsDao()
                // Get all subscriptions
                .getAll()
                // Convert to JSON each one
                .map { it.toJSON() }
                // Convert into a JSONArray
                .toJSONArray()
            val credentialsJson = database
                .credentialsDao()
                // Get all credentials
                .getAll()
                // Convert to JSON each one
                .map { it.toJSON() }
                // Convert into a JSONArray
                .toJSONArray()
            return JSONObject().apply {
                put("subscriptions", subscriptionsJson)
                put("credentials", credentialsJson)
            }
        }
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {

    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        TODO("Not yet implemented")
    }
}