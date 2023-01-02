package at.bitfire.icsdroid.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets or instantiates a reference to the database's singleton.
         * @param context The application's context.
         * @return An instance of [AppDatabase].
         */
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "icsx5").build().also {  INSTANCE = it }
        }
    }

    abstract fun subscriptionsDao(): SubscriptionsDao

    abstract fun eventsDao(): EventsDao
}
