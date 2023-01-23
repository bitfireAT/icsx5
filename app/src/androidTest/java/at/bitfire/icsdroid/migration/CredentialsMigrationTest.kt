@file:Suppress("DEPRECATION")

package at.bitfire.icsdroid.migration

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.dao.get
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.runBlocking
import org.junit.*

class CredentialsMigrationTest {
    companion object {
        private lateinit var appContext: Context

        @BeforeClass
        @JvmStatic
        fun setUpContext() {
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    /** Provides an in-memory interface to the app's database */
    private lateinit var database: AppDatabase
    private lateinit var credentialsDao: CredentialsDao
    private lateinit var subscriptionsDao: SubscriptionsDao
    private lateinit var calendarCredentials: CalendarCredentials

    private val account = Account("LocalCalendarTest", CalendarContract.ACCOUNT_TYPE_LOCAL)

    private val username: String = "randomUsername"
    private val password: String = "randomPassword"
    private val subscription = Subscription(123, Uri.EMPTY, null, "Test subscription", account)

    // Initialize the Room database
    @Before
    fun prepareDatabase() {
        Assert.assertNotNull(appContext)

        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        credentialsDao = database.credentialsDao()
        subscriptionsDao = database.subscriptionsDao()

        AppDatabase.setInstance(database)
    }

    // Add the sample subscription to the database
    @Before
    fun prepareSubscription() {
        runBlocking { subscriptionsDao.add(subscription) }
    }

    // Insert some credentials that would have been stored in shared preferences
    @Before
    fun prepareOldCredentials() {
        calendarCredentials = CalendarCredentials(appContext)
        calendarCredentials.put(subscription, username, password)
    }

    @Test
    fun runMigration() {
        val worker = TestListenableWorkerBuilder<SyncWorker>(
            context = appContext,
        ).setInputData(
            workDataOf(
                // Choose the correct account type
                SyncWorker.ACCOUNT_NAME to account.name,
                SyncWorker.ACCOUNT_TYPE to account.type,
            ),
        ).build()

        runBlocking {
            val result = worker.doWork()
            Assert.assertEquals(result, ListenableWorker.Result.success())

            // Check that the credentials dao now has the credential
            val cred = credentialsDao.get(subscription)
            Assert.assertNotNull(cred)
            Assert.assertEquals(cred?.username, username)
            Assert.assertEquals(cred?.password, password)

            // Check that the credentials have been removed from CalendarCredentials
            val oldCred = calendarCredentials.get(subscription)
            Assert.assertNull(oldCred.first)
            Assert.assertNull(oldCred.second)
        }
    }

    // Remove the created subscription from the database
    @After
    fun removeSubscription() {
        runBlocking { subscriptionsDao.delete(subscription) }
    }
}