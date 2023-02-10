/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid.migration

import android.Manifest
import android.content.ContentProviderClient
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
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
import at.bitfire.icsdroid.CalendarFetcherTest
import at.bitfire.icsdroid.SyncWorker
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.LocalCalendar
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.test.R
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

        private lateinit var appContext: Context

        private lateinit var provider: ContentProviderClient

        @BeforeClass
        @JvmStatic
        fun setUpProvider() {
            appContext = InstrumentationRegistry.getInstrumentation().targetContext
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

        AppDatabase.setInstance(database)
    }

    private lateinit var calendar: LocalCalendar

    @Before
    fun prepareCalendar() {
        val account = AppAccount.get(appContext)
        val resUri = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${CalendarFetcherTest.testContext.packageName}/${R.raw.vienna_evolution}"
        val uri = AndroidCalendar.create(
            account,
            provider,
            contentValuesOf(
                CalendarContract.Calendars.NAME to resUri,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME to "LocalCalendarTest",
            ),
        )
        calendar = AndroidCalendar.findByID(
            account,
            provider,
            LocalCalendar.Factory,
            ContentUris.parseId(uri)
        )
    }

    @After
    fun shutdown() {
        calendar.delete()
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
            assertNotNull(subscriptions.find { it.id == calendar.id })
        }
    }
}
