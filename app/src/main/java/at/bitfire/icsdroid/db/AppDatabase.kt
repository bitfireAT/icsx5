/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The database for storing all the ICSx5 subscriptions and other data.
 */
@TypeConverters(Converters::class)
@Database(
    entities = [Subscription::class, Credential::class],
    version = 5,
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
        ),
        AutoMigration (
            from = 4,
            to = 5
        )
    ]
)
abstract class AppDatabase : RoomDatabase() {

    @Module
    @InstallIn(SingletonComponent::class)
    object AppDatabaseModule {

        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase = Room
            .databaseBuilder(context, AppDatabase::class.java, "icsx5")
            .fallbackToDestructiveMigration(true)
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    SyncWorker.run(context)
                }
            })
            .build()

    }

    abstract fun subscriptionsDao(): SubscriptionsDao
    abstract fun credentialsDao(): CredentialsDao

}
