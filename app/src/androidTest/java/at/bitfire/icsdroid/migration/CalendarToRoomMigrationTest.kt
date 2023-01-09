package at.bitfire.icsdroid.migration

import android.accounts.Account
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker.Result
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.InitCalendarProviderRule
import at.bitfire.icsdroid.R
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Subscription
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.rules.TestRule
import kotlin.random.Random

class CalendarToRoomMigrationTest {
    companion object {
        @JvmField
        @ClassRule
        val initCalendarProviderRule: TestRule = InitCalendarProviderRule.getInstance()

        val appContext: Context by lazy { InstrumentationRegistry.getInstrumentation().targetContext }
        val testContext: Context by lazy { InstrumentationRegistry.getInstrumentation().context }

        private lateinit var provider: ContentProviderClient

        @BeforeClass
        @JvmStatic
        fun setUpProvider() {
            provider = appContext.contentResolver.acquireContentProviderClient(CalendarContract.AUTHORITY)!!
        }

        @AfterClass
        @JvmStatic
        fun closeProvider() {
            provider.closeCompat()
        }
    }

    /** Provides an in-memory interface to the app's database */
    private lateinit var database: AppDatabase
    private lateinit var dao: SubscriptionsDao

    /** The id of the calendar to be created */
    private var calendarId = Random.nextLong()

    // Initialize the test WorkManager for scheduling workers
    @Before
    fun prepareWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)
    }

    // Initialize the Room database
    @Before
    fun prepareDatabase() {
        assertNotNull(appContext)

        database = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        dao = database.subscriptionsDao()

        @Suppress("DEPRECATION")
        AppDatabase.setInstance(database)
    }

    @Before
    fun prepareOldCalendar() {
        // We add a calendar before running the worker. If everything works fine, a subscription
        // shall be created automatically.
        val account = Account(
            appContext.getString(R.string.account_name),
            appContext.getString(R.string.account_type),
        )
        val uri =
            Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${testContext.packageName}/${at.bitfire.icsdroid.test.R.raw.vienna_evolution}")
        AndroidCalendar.create(
            account,
            provider,
            contentValuesOf(
                CalendarContract.Calendars._ID to calendarId,
                CalendarContract.Calendars.ACCOUNT_NAME to account.name,
                CalendarContract.Calendars.ACCOUNT_TYPE to account.type,
                CalendarContract.Calendars.NAME to uri.toString(),
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME to "Vienna Evolution",
                CalendarContract.Calendars.CALENDAR_COLOR to Subscription.DEFAULT_COLOR,
                CalendarContract.Calendars.OWNER_ACCOUNT to account.name,
                CalendarContract.Calendars.SYNC_EVENTS to 1,
                CalendarContract.Calendars.VISIBLE to 1,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL to CalendarContract.Calendars.CAL_ACCESS_READ,
            ),
        )
    }

    @Test
    fun testSubscriptionCreated() {
        val worker = TestListenableWorkerBuilder<SyncWorker>(
            context = appContext,
        ).build()

        runBlocking {
            val result = worker.doWork()
            assertEquals(result, Result.success())

            // Get all the subscriptions
            val subscriptions = dao.getAll()
            // Check that the created calendar has been added to the subscriptions list
            assertNotNull(subscriptions.find { it.id == calendarId })
        }
    }
}
