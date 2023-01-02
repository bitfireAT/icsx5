package at.bitfire.icsdroid.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import at.bitfire.icsdroid.db.AppDatabase.Companion.getInstance
import at.bitfire.icsdroid.db.dao.EventsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.db.entity.SubscriptionEvent

/**
 * The database for storing all the ICSx5 subscriptions and other data. Use [getInstance] for getting access to the database.
 */
@Database(entities = [Subscription::class, SubscriptionEvent::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

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
                val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "icsx5").build()
                instance = db
                return db
            }
        }
    }

    abstract fun subscriptionsDao(): SubscriptionsDao
    abstract fun eventsDao(): EventsDao

}
