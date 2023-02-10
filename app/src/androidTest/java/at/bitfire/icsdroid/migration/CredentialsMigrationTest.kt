/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

@file:Suppress("DEPRECATION")

package at.bitfire.icsdroid.migration

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.DatabaseAndroidInterface
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CredentialsMigrationTest {
    companion object {
        private lateinit var appContext: Context

        // Initialize the app context access
        @BeforeClass
        @JvmStatic
        fun setUpContext() {
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    /** Provides an in-memory interface to the app's database */
    private lateinit var database: AppDatabase
    /** Provides an interface for storing credentials into [database] */
    private lateinit var credentialsDao: CredentialsDao
    /** Provides an interface for storing subscriptions into [database] */
    private lateinit var subscriptionsDao: SubscriptionsDao
    /** Provides access to the old credentials storage class */
    private lateinit var calendarCredentials: CalendarCredentials

    /** A sample username for the credentials to check for */
    private val username: String = "randomUsername"
    /** A sample password for the credentials to check for */
    private val password: String = "randomPassword"
    /** A sample subscription for the credentials to check for */
    private val subscription = Subscription(123, Uri.EMPTY, null, "Test subscription")

    /** For interfacing between the Android system and Room */
    private val databaseAndroidInterface = DatabaseAndroidInterface(appContext, subscription)

    // Initialize the Room database
    @Before
    fun cPrepareDatabase() {
        // Make sure there's a non-null context initialized
        Assert.assertNotNull(appContext)

        // Load an in-memory database
        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        // Give access through CredentialsDao
        credentialsDao = database.credentialsDao()
        // Give access through SubscriptionsDao
        subscriptionsDao = database.subscriptionsDao()

        // Set the instance of AppDatabase to the initialized class
        AppDatabase.setInstance(database)
    }

    @Before
    fun bPrepareSubscription() {
        // Add the sample subscription to the database
        subscriptionsDao.add(subscription)
        // And add it to the system's calendar
        databaseAndroidInterface.createAndroidCalendar()
    }

    @Before
    fun aPrepareOldCredentials() {
        // Initialize the CalendarCredentials
        calendarCredentials = CalendarCredentials(appContext)
        // Insert some credentials that would have been stored in shared preferences
        calendarCredentials.put(databaseAndroidInterface.getCalendar(), username, password)
    }

    @Test
    fun runMigration() {
        // Create a testing instance of SyncWorker for running the synchronization
        val worker = TestListenableWorkerBuilder<SyncWorker>(
            context = appContext,
        ).build()

        runBlocking {
            // Run the worker
            val result = worker.doWork()
            // Make sure the worker ran correctly
            Assert.assertEquals(result, ListenableWorker.Result.success())

            // Check that the credentials dao now has the credential
            val cred = credentialsDao.getBySubscriptionId(subscription.id)
            Assert.assertNotNull(cred)
            Assert.assertEquals(cred?.username, username)
            Assert.assertEquals(cred?.password, password)

            // Check that the credentials have been removed from CalendarCredentials
            val oldCred = calendarCredentials.get(databaseAndroidInterface.getCalendar())
            Assert.assertNull(oldCred.first)
            Assert.assertNull(oldCred.second)
        }
    }

    @After
    fun removeSubscription() {
        // Remove the calendar from the system
        databaseAndroidInterface.deleteAndroidCalendar()
        // Remove the created subscription from the database
        subscriptionsDao.delete(subscription)
    }
}
