/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import at.bitfire.icsdroid.Constants
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase.Companion.getInstance
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import java.io.InputStream
import java.util.concurrent.Callable
import kotlinx.coroutines.delay

/**
 * The database for storing all the ICSx5 subscriptions and other data. Use [getInstance] for getting access to the database.
 */
@TypeConverters(Converters::class)
@Database(
    entities = [Subscription::class, Credential::class],
    version = 3,
    autoMigrations = [
        AutoMigration (
            from = 1,
            to = 2
        ),
        AutoMigration (
            from = 2,
            to = 3
        )
    ]
)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /**
         * This function is only intended to be used by tests, use [getInstance], it initializes
         * the instance automatically.
         */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        fun setInstance(instance: AppDatabase?) {
            this.instance = instance
        }

        /**
         * Gets or instantiates the database singleton. Thread-safe.
         * @param context The application's context, required to create the database.
         */
        fun getInstance(context: Context): AppDatabase {
            // if we have an existing instance, return it
            instance?.let {
                return it
            }

            // multiple threads might access this code at once, so synchronize it
            synchronized(AppDatabase) {
                // another thread might just have created an instance
                instance?.let {
                    return it
                }

                // create a new instance and save it
                val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "icsx5")
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            SyncWorker.run(context, onlyMigrate = true)
                        }
                    })
                    .build()
                instance = db
                return db
            }
        }

        /** Reads all the data stored in the database */
        suspend fun readAllData(context: Context): ByteArray {
            // Wait until transaction is finished
            if (instance != null) while (instance?.inTransaction() == true) { delay(1) }
            // Close access to the database so no writes are performed
            instance?.close()

            // Get access to the database file
            val file = context.getDatabasePath("icsx5")
            // Read the contents
            val bytes = file.readBytes()

            // Open the database again
            getInstance(context)

            // Return the read bytes
            return bytes
        }

        /** Clears the current database, and creates a new one from [stream] */
        fun recreateFromFile(context: Context, stream: Callable<InputStream>) {
            // Clear all the data existing if any
            Log.d(Constants.TAG, "Clearing all tables in the database...")
            instance?.clearAllTables()
            instance?.close()
            instance = null

            Log.d(Constants.TAG, "Creating a new database from the data imported...")
            val newDatabase = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "icsx5")
                .createFromInputStream(stream)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        SyncWorker.run(context, onlyMigrate = true)
                    }
                })
                .build()

            val subscriptions = newDatabase.subscriptionsDao().getAll()
            Log.i(Constants.TAG, "Successfully imported ${subscriptions.size} subscriptions.")

            instance = newDatabase
        }
    }

    abstract fun subscriptionsDao(): SubscriptionsDao
    abstract fun credentialsDao(): CredentialsDao

}
