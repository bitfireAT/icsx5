/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.db

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase.Companion.getInstance
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.runBlocking

/**
 * The database for storing all the ICSx5 subscriptions and other data. Use [getInstance] for getting access to the database.
 */
@TypeConverters(Converters::class)
@Database(
    entities = [Subscription::class, Credential::class],
    version = 4,
    autoMigrations = [
        AutoMigration (
            from = 1,
            to = 2
        ),
        AutoMigration (
            from = 2,
            to = 3
        ),
        AutoMigration (
            from = 3,
            to = 4
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
                            runBlocking { SyncWorker.run(context, onlyMigrate = true) }
                        }
                    })
                    .build()
                instance = db
                return db
            }
        }
    }

    abstract fun subscriptionsDao(): SubscriptionsDao
    abstract fun credentialsDao(): CredentialsDao

}
