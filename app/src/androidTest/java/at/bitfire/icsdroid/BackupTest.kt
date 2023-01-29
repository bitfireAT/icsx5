package at.bitfire.icsdroid

import android.accounts.Account
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.Backup
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.dao.put
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.utils.mapJSONObjects
import kotlinx.coroutines.runBlocking
import org.junit.*

class BackupTest {
    companion object {
        private lateinit var appContext: Context

        @BeforeClass
        @JvmStatic
        fun setUpProvider() {
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    /** Provides an in-memory interface to the app's database */
    private lateinit var database: AppDatabase
    private lateinit var dao: SubscriptionsDao

    private val account = Account("LocalCalendarTest", CalendarContract.ACCOUNT_TYPE_LOCAL)

    private val sampleSubscription1 = Subscription(
        1000,
        Uri.parse("https://example.com/1"),
        null,
        "Testing Subscription 1",
        account.name,
        account.type,
        0L,
        0L,
        false,
        null,
        false,
        null,
        Subscription.DEFAULT_COLOR,
        isSynced = true,
        isVisible = true,
    )

    private val sampleSubscription2 = Subscription(
        1001,
        Uri.parse("https://example.com/2"),
        null,
        "Testing Subscription 2",
        account.name,
        account.type,
        0L,
        0L,
        false,
        null,
        false,
        null,
        Subscription.DEFAULT_COLOR,
        isSynced = true,
        isVisible = true,
    )

    private val sampleCredential = Credential(1000, "username", "password123")

    // Initialize the Room database
    @Before
    fun prepareDatabase() {
        Assert.assertNotNull(appContext)

        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        dao = database.subscriptionsDao()

        AppDatabase.setInstance(database)
    }

    @Before
    fun prepareData() = runBlocking {
        // Insert some sample subscriptions
        val subscriptionsDao = database.subscriptionsDao()
        subscriptionsDao.add(sampleSubscription1, sampleSubscription2)

        // Insert some sample credentials
        val credentialsDao = database.credentialsDao()
        credentialsDao.put(sampleCredential)
    }

    @Test
    fun testBackupCreation() {
        // Create a backup JSON
        val backup = runBlocking { Backup.createBackup(appContext) }
        // Get all subscriptions that have been backup, and map them into JSONObjects
        val subscriptions = backup.getJSONArray("subscriptions").mapJSONObjects()
        // Same for credentials
        val credentials = backup.getJSONArray("credentials").mapJSONObjects()

        // Find each one of the subscriptions, and make sure they are found
        val subscription1 = subscriptions.find { it.getLong("id") == 1000L }
        val subscription2 = subscriptions.find { it.getLong("id") == 1001L }
        Assert.assertNotNull(subscription1)
        Assert.assertNotNull(subscription2)

        // Find each one of the credentials, and make sure they are found
        val credential = credentials.find { it.getLong("subscriptionId") == 1000L }
        Assert.assertNotNull(credential)

        // Make sure the data that has been backed up matches the one that should be there
        Assert.assertEquals(sampleSubscription1.toJSON().toString(), subscription1.toString())
        Assert.assertEquals(sampleSubscription2.toJSON().toString(), subscription2.toString())
        Assert.assertEquals(sampleCredential.toJSON().toString(), credential.toString())
    }

    @After
    fun flushData() = runBlocking {
        // Remove all the data that has been added
        database.subscriptionsDao().delete(sampleSubscription1, sampleSubscription2)
        database.credentialsDao().pop(sampleCredential.subscriptionId)
    }
}