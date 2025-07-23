/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package at.bitfire.icsdroid

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
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
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.test.R
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import javax.inject.Inject

@HiltAndroidTest
class BaseSyncWorkerTest {

    @Inject @ApplicationContext
    lateinit var applicationContext: Context

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @get:Rule
    val calendarPermissionRule: GrantPermissionRule? = GrantPermissionRule.grant(
        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
    )

    @Before
    fun setUp() {
        hiltRule.inject()

        // Initialize the test WorkManager for scheduling workers
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setWorkerFactory(workerFactory)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(applicationContext, config)
    }

    @After
    fun closeDatabase() {
        db.clearAllTables()
        db.close()
    }

    private suspend fun runWorkerAndGetResult(): WorkInfo? {
        // Run the worker
        val uuid = UUID.randomUUID()
        val request = OneTimeWorkRequestBuilder<TestSyncWorker>()
            .setId(uuid)
            .setInputData(workDataOf(FORCE_RESYNC to true))
            .build()
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(request).await()

        // Block the thread until completed
        var workInfo: WorkInfo? = null
        withTimeout(5_000) {
            try {
                runBlocking {
                    workManager.getWorkInfoByIdFlow(uuid).filterNotNull().collect {
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
        db.subscriptionsDao().add(
            Subscription(
                url = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}.test/${R.raw.sample}"),
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
        db.subscriptionsDao().add(
            Subscription(
                url = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${BuildConfig.APPLICATION_ID}.test/${R.raw.sample}"),
                displayName = "Local Subscription"
            )
        )
        // Insert a remote subscription that doesn't exist (failure)
        db.subscriptionsDao().add(
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
