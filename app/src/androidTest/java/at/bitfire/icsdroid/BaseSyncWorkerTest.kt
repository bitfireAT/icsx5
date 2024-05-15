package at.bitfire.icsdroid

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import at.bitfire.icsdroid.BaseSyncWorker.Companion.FORCE_RESYNC
import at.bitfire.icsdroid.BaseSyncWorker.Companion.ONLY_MIGRATE
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.dao.SubscriptionsDao
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.migration.CalendarToRoomMigrationTest
import at.bitfire.icsdroid.test.BuildConfig
import at.bitfire.icsdroid.test.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BaseSyncWorkerTest {

    companion object {
        @JvmField
        @ClassRule
        val calendarPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
        )
    }
    private val applicationContext: Context get() = ApplicationProvider.getApplicationContext()

    /** Provides an in-memory interface to the app's database */
    private lateinit var db: AppDatabase
    private lateinit var subscriptionsDao: SubscriptionsDao

    // Initialize the test WorkManager for scheduling workers
    @Before
    fun prepareWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(CalendarToRoomMigrationTest.appContext, config)
    }

    // Initialize the Room database
    @Before
    fun prepareDatabase() {
        Assert.assertNotNull(CalendarToRoomMigrationTest.appContext)

        db = Room.inMemoryDatabaseBuilder(CalendarToRoomMigrationTest.appContext, AppDatabase::class.java).build()
        subscriptionsDao = db.subscriptionsDao()

        AppDatabase.setInstance(db)
    }

    @After
    @Throws(IOException::class)
    fun closeDatabase() {
        db.clearAllTables()
        db.close()
        AppDatabase.setInstance(null)
    }

    private suspend fun runWorkerAndGetResult(): WorkInfo? {
        // Run the worker
        val uuid = UUID.randomUUID()
        val request = OneTimeWorkRequestBuilder<BaseSyncWorker>()
            .setId(uuid)
            .setInputData(
                workDataOf(FORCE_RESYNC to true, ONLY_MIGRATE to false)
            )
            .build()
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(request).await()

        // Block the thread until completed
        var workInfo: WorkInfo? = null
        withTimeout(5_000) {
            try {
                runBlocking {
                    workManager.getWorkInfoByIdFlow(uuid).collect {
                        if (it.state.isFinished) {
                            workInfo = it
                            cancel()
                        }
                    }
                }
            } catch (_: CancellationException) {
                // ignore cancellations since it's expected
            }
        }
        return workInfo
    }

    @Test
    fun syncSingleLocal() = runBlocking {
        // Insert a sample subscription
        subscriptionsDao.add(
            Subscription(
                url = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/${R.raw.sample}"),
                displayName = "Local Subscription"
            )
        )

        // Run the worker
        val workInfo = runWorkerAndGetResult()

        assert(workInfo!!.state == WorkInfo.State.SUCCEEDED) {
            "Expected the sync to succeed, but got ${workInfo.state}"
        }
    }

    @Test
    fun syncLocalAndRemoteNotFound() = runBlocking {
        // Insert a local subscription (success)
        subscriptionsDao.add(
            Subscription(
                url = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}/${R.raw.sample}"),
                displayName = "Local Subscription"
            )
        )
        // Insert a remote subscription that doesn't exist (failure)
        subscriptionsDao.add(
            Subscription(
                url = Uri.parse("https://example.com/invalid.ics"),
                displayName = "Remote Subscription"
            )
        )

        // Run the worker
        val workInfo = runWorkerAndGetResult()

        // The sync is expected to fail
        assert(workInfo!!.state == WorkInfo.State.FAILED) {
            "Expected the sync to fail, but got ${workInfo.state}"
        }
    }
}
