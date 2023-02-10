/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.migration

import android.Manifest
import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract.Calendars
import android.util.Log
import androidx.core.content.contentValuesOf
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.ListenableWorker.Result
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.AppAccount
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.dao.CredentialsDao
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

class CalendarToRoomMigrationTest {

    companion object {
        @JvmField
        @ClassRule
        val calendarPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )

        val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        private lateinit var provider: ContentProviderClient

        @BeforeClass
        @JvmStatic
        fun setUpProvider() {
            provider = LocalCalendar.getCalendarProvider(appContext)
        }

        @AfterClass
        @JvmStatic
        fun closeProvider() {
            provider.closeCompat()
        }

        const val CALENDAR_DISPLAY_NAME = "Some subscription"
        const val CALENDAR_URL = "https://example.com/test.ics"
        const val CALENDAR_USERNAME = "someUser"
        const val CALENDAR_PASSWORD = "somePassword"

    }

    /** Provides an in-memory interface to the app's database */
    private lateinit var db: AppDatabase
    private lateinit var credentialsDao: CredentialsDao
    private lateinit var subscriptionsDao: SubscriptionsDao

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

        db = Room.inMemoryDatabaseBuilder(appContext, AppDatabase::class.java).build()
        credentialsDao = db.credentialsDao()
        subscriptionsDao = db.subscriptionsDao()

        AppDatabase.setInstance(db)
    }


    private fun createCalendar(): LocalCalendar {
        val account = AppAccount.get(appContext)
        val uri = AndroidCalendar.create(
            account,
            provider,
            contentValuesOf(
                Calendars.CALENDAR_DISPLAY_NAME to CALENDAR_DISPLAY_NAME,
                Calendars.NAME to CALENDAR_URL
            )
        )

        val calendar = AndroidCalendar.findByID(
            account,
            provider,
            LocalCalendar.Factory,
            ContentUris.parseId(uri)
        )

        // associate credentials, too
        CalendarCredentials(appContext).put(calendar, CALENDAR_USERNAME, CALENDAR_PASSWORD)

        return calendar
    }

    @Test
    fun testSubscriptionCreated() {
        val worker = TestListenableWorkerBuilder<SyncWorker>(
            context = appContext
        ).build()

        val calendar = createCalendar()
        try {
            runBlocking {
                val result = worker.doWork()
                assertEquals(result, Result.success())

                val subscription = subscriptionsDao.getAll().first()
                // check that the calendar has been added to the subscriptions list
                assertEquals(calendar.id, subscription.id)
                assertEquals(CALENDAR_DISPLAY_NAME, subscription.displayName)
                assertEquals(Uri.parse(CALENDAR_URL), subscription.url)

                // check credentials, too
                val credentials = credentialsDao.getBySubscriptionId(subscription.id)
                assertEquals(CALENDAR_USERNAME, credentials?.username)
                assertEquals(CALENDAR_PASSWORD, credentials?.password)
            }
        } finally {
            calendar.delete()
        }
    }

}