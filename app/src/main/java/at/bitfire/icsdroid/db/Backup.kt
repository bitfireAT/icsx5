package at.bitfire.icsdroid.db

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.content.Context
import android.database.SQLException
import android.os.ParcelFileDescriptor
import androidx.annotation.WorkerThread
import at.bitfire.icsdroid.db.dao.put
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.utils.mapJSONObjects
import at.bitfire.icsdroid.utils.matches
import at.bitfire.icsdroid.utils.toJSONArray
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*

class Backup : BackupAgent() {
    companion object {
        /** Informs of the current agent version. Used for version bumps. */
        private const val AGENT_VERSION = 1

        /** The data key where the backup is stored at */
        private const val APP_DATA_KEY = "icsx5_data"

        /** The name used by the backup file */
        private const val BACKUP_FILE_NAME = "backup.json"

        /**
         * Converts all the data stored in the database into a [JSONObject].
         * @param context The [Context] that is making the request.
         * @return A [JSONObject] with two keys, `subscriptions` and `credentials`. Each of them
         * contains a [JSONArray] that contain all the subscriptions and all the credentials
         * respectively.
         */
        @WorkerThread
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

        /**
         * Stores the backup obtained into the database. Data already stored in the database will
         * not be overridden.
         *
         * If a subscription is found with an older [Subscription.lastModified] or
         * [Subscription.lastSync], it will be overwritten.
         * @param context The context that is requesting to restore the database.
         * @param backup The backup contents.
         * @param clear If `true`, the database will be cleared, and only the contents of [backup]
         * will be present.
         * @throws SQLException If there's a problem while writing into the database.
         * @throws JSONException If there's a problem while parsing the contents of [backup].
         */
        @WorkerThread
        suspend fun restoreBackup(context: Context, backup: JSONObject, clear: Boolean = false) {
            val database = AppDatabase.getInstance(context)
            val subscriptionsDao = database.subscriptionsDao()
            val credentialsDao = database.credentialsDao()

            if (clear) database.nuke()

            // Write all the subscriptions into the database
            val subscriptions = backup.getJSONArray("subscriptions").mapJSONObjects()
            for (subscription in subscriptions) {
                val newSubscription = Subscription.fromJSON(subscription)

                // Check if there's any credential for the subscription id
                val dbSubscription = subscriptionsDao.getById(subscription.getLong("id"))
                if (dbSubscription == null) {
                    // If there is not a subscription stored, write it
                    subscriptionsDao.add(newSubscription)
                } else {
                    // If there's already a subscription stored, check if the one we have got is newer
                    val lastModified = dbSubscription.lastModified
                    val lastSync = dbSubscription.lastSync
                    if (newSubscription.lastModified < lastModified || newSubscription.lastSync < lastSync) {
                        // The one in the backup is newer, overwrite
                        subscriptionsDao.add(newSubscription)
                    }
                }
            }

            // Write all the credentials into the database
            val credentials = backup.getJSONArray("credentials").mapJSONObjects()
            for (credential in credentials) {
                // Check if there's any credential for the subscription id
                val dbCredential = credentialsDao.get(credential.getLong("subscriptionId"))
                if (dbCredential == null) {
                    // If there is not a credential stored for that subscription, write it
                    credentialsDao.put(Credential.fromJSON(credential))
                }
            }
        }
    }

    /** The file to store the data at */
    private lateinit var dataFile: File

    /** Stores the data created for the new backup using [createBackup]. */
    private lateinit var newBackupData: JSONObject

    /** Stores the new backup data to be stored. Will be JSON */
    private lateinit var newBackup: String

    override fun onCreate() {
        super.onCreate()
        dataFile = File(filesDir, BACKUP_FILE_NAME)
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput,
        newState: ParcelFileDescriptor,
    ) {
        // Remove the data file if it exists
        if (dataFile.exists()) dataFile.delete()
        // Create parent directory just in case
        dataFile.parentFile?.mkdirs()

        // Create the backup data
        newBackupData = runBlocking { createBackup(this@Backup) }
        // Write some attributes
        val backup = JSONObject().apply {
            put("agentVersion", AGENT_VERSION)
            put("data", newBackupData)
        }
        newBackup = backup.toString()

        var doBackup = oldState == null
        if (!doBackup)
            doBackup = compareStateFile(oldState!!)
        if (doBackup) {
            val bufStream = ByteArrayOutputStream()
            bufStream.bufferedWriter().write(backup.toString())

            val buffer = bufStream.toByteArray()
            val len = buffer.size
            data.writeEntityHeader(APP_DATA_KEY, len)
            data.writeEntityData(buffer, len)
        }

        writeStateFile(newState)
    }

    override fun onRestore(
        data: BackupDataInput,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        while (data.readNextHeader()) {
            val key = data.key
            val size = data.dataSize

            if (key == APP_DATA_KEY) {
                // Read the data from the entity
                val dataBuf = ByteArray(size)
                data.readEntityData(dataBuf, 0, size)
                val inStream = dataBuf.inputStream()
                val raw = inStream.reader().readText()
                // Convert into json, and get the data field
                val json = JSONObject(raw)
                val jsonData = json.getJSONObject("data")
                // Restore the backup
                runBlocking { restoreBackup(this@Backup, jsonData) }
            } else
                data.skipEntityData()
        }
    }

    /**
     * Helper routine - read a previous state file and decide whether to
     * perform a backup based on its contents.
     *
     * @return `true` if the application's data has changed since
     * the last backup operation; `false` otherwise.
     */
    private fun compareStateFile(oldState: ParcelFileDescriptor): Boolean {
        val inputStream = FileInputStream(oldState.fileDescriptor)
        val data = DataInputStream(inputStream)
        val inputJson = JSONObject(data.reader().readText())
        return try {
            val stateVersion: Int = inputJson.getInt("agentVersion")
            if (stateVersion > AGENT_VERSION) {
                // Whoops; the last version of the app that backed up
                // data on this device was <em>newer</em> than the current
                // version -- the user has downgraded.  That's problematic.
                // In this implementation, we recover by simply rewriting
                // the backup.
                return true
            }
            // The state data we store is just a mirror of the app's data;
            // read it from the state file then return 'true' if any of
            // it differs from the current data.
            val lastSubscriptions = inputJson.getJSONArray("subscriptions")
            val lastCredentials = inputJson.getJSONArray("credentials")
            val newSubscriptions = newBackupData.getJSONArray("subscriptions")
            val newCredentials = newBackupData.getJSONArray("credentials")
            return !lastSubscriptions.matches(newSubscriptions) || !lastCredentials.matches(
                newCredentials
            )
        } catch (e: IOException) {
            // If something went wrong reading the state file, be safe
            // and back up the data again.
            true
        }
    }

    /**
     * Writes the new state file to the given descriptor. Takes the data from [newBackup].
     */
    private fun writeStateFile(stateFile: ParcelFileDescriptor) {
        val outStream = FileOutputStream(stateFile.fileDescriptor)
        DataOutputStream(outStream)
            // Create a buffered writer for writing into the file
            .bufferedWriter()
            // Write the contents generated
            .write(newBackup)
    }
}