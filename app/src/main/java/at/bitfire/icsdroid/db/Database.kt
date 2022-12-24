package at.bitfire.icsdroid.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Subscription

/**
 * The database for storing all the ICSx5 subscriptions and other data. Use [getInstance] for getting access to the database.
 * @since 20221206
 */
@Database(entities = [Subscription::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Gets or instantiates a reference to the database's singleton.
         * @since 20221216
         * @param context The application's context.
         * @return An instance of [AppDatabase].
         */
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "icsx5").build().also {  INSTANCE = it }
        }
    }

    abstract fun subscriptionsDao(): SubscriptionsDao
}
