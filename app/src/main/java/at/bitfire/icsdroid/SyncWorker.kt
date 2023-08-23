/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.work.*
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.NotificationUtils

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
): CoroutineWorker(context, workerParams) {

    companion object {

        /** The name of the worker. Tags the unique work. */
        const val NAME = "SyncWorker"

        /**
         * An input data (Boolean) for the Worker that tells whether the synchronization should be performed
         * without taking into account the current network condition.
         */
        const val FORCE_RESYNC = "forceResync"

        /**
         * An input data (Boolean) for the Worker that tells if only migration should be performed, without
         * fetching data.
         */
        const val ONLY_MIGRATE = "onlyMigration"

        const val PROGRESS_STEP = "step"
        const val PROGRESS_MAX = "max"
        const val PROGRESS_CURRENT = "current"
        const val PROGRESS_SUBSCRIPTION = "subscription"

        /**
         * Enqueues a sync job for immediate execution. If the sync is forced,
         * the "requires network connection" constraint won't be set.
         *
         * @param context      required for managing work
         * @param force        *true* enqueues the sync regardless of the network state; *false* adds a [NetworkType.CONNECTED] constraint
         * @param forceResync  *true* ignores all locally stored data and fetched everything from the server again
         * @param onlyMigrate  *true* only runs synchronization, without fetching data.
         */
        fun run(context: Context, force: Boolean = false, forceResync: Boolean = false, onlyMigrate: Boolean = false) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInputData(
                    workDataOf(
                        FORCE_RESYNC to forceResync,
                        ONLY_MIGRATE to onlyMigrate,
                    )
                )

            val policy: ExistingWorkPolicy = if (force) {
                Log.i(TAG, "Manual sync, ignoring network condition")

                // overwrite existing syncs (which may have unwanted constraints)
                ExistingWorkPolicy.REPLACE
            } else {
                // regular sync, requires network
                request.setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build())

                // don't overwrite previous syncs (whether regular or manual)
                ExistingWorkPolicy.KEEP
            }

            WorkManager.getInstance(context)
                    .beginUniqueWork(NAME, policy, request.build())
                    .enqueue()
        }

        fun liveStatus(context: Context) =
            WorkManager.getInstance(context).getWorkInfosForUniqueWorkLiveData(NAME)

    }

    private val database = AppDatabase.getInstance(applicationContext)
    private val subscriptionsDao = database.subscriptionsDao()
    private val credentialsDao = database.credentialsDao()

    private val account = AppAccount.get(applicationContext)
    lateinit var provider: ContentProviderClient

    private lateinit var foregroundInfo: ForegroundInfo

    private var forceReSync: Boolean = false

    override suspend fun doWork(): Result {
        forceReSync = inputData.getBoolean(FORCE_RESYNC, false)
        val onlyMigrate = inputData.getBoolean(ONLY_MIGRATE, false)
        Log.i(TAG, "Synchronizing (forceReSync=$forceReSync,onlyMigrate=$onlyMigrate)")

        updateForegroundInfo(SyncSteps.Start)

        provider =
            try {
                LocalCalendar.getCalendarProvider(applicationContext)
            } catch (e: SecurityException) {
                NotificationUtils.showCalendarPermissionNotification(applicationContext)
                return Result.failure()
            }

        try {
            // migrate old calendar-based subscriptions to database
            migrateLegacyCalendars()

            // Do not synchronize if onlyMigrate is true
            if (onlyMigrate) return Result.success()

            // update local calendars according to the subscriptions
            updateLocalCalendars()

            // provide iCalendar event color values to Android
            val account = AppAccount.get(applicationContext)
            AndroidCalendar.insertColors(provider, account)

            // sync local calendars
            val subscriptions = subscriptionsDao.getAll()
            for ((i, subscription) in subscriptions.withIndex()) {
                // Make sure the subscription has a matching calendar
                subscription.calendarId ?: continue

                updateForegroundInfo(SyncSteps.Calendar(subscriptions.size, i, subscription.id))

                val calendar = LocalCalendar.findById(account, provider, subscription.calendarId)
                ProcessEventsTask(applicationContext, subscription, calendar, forceReSync)
                    .sync { progress, max ->
                        updateForegroundInfo(
                            SyncSteps.ProcessEvents(max, progress, subscription.id)
                        )
                    }
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thread interrupted", e)
            return Result.retry()
        } finally {
            provider.closeCompat()
        }

        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo

    /**
     * Migrates all the legacy calendar-based subscriptions to the database. Performs these steps:
     *
     * 1. Searches for all the calendars created
     * 2. Checks that those calendars have a matching [Subscription] in the database.
     * 3. If there's no matching [Subscription], create it.
     */
    private suspend fun migrateLegacyCalendars() {
        updateForegroundInfo(SyncSteps.Migration)

        @Suppress("DEPRECATION")
        val legacyCredentials by lazy { CalendarCredentials(applicationContext) }

        // if there's a provider available, get all the calendars available in the system
        for (calendar in LocalCalendar.findUnmanaged(account, provider)) {
            Log.i(TAG, "Found unmanaged (<= v2.1.1) calendar ${calendar.id}, migrating")
            @Suppress("DEPRECATION")
            val url = calendar.url ?: continue

            // Special case v2.1: it created subscriptions, but did not set the COLUMN_MANAGED_BY_DB flag.
            val subscription = subscriptionsDao.getByUrl(url)
            if (subscription != null) {
                // So we already have a subscription and only net to set its calendar_id.
                Log.i(TAG, "Migrating from v2.1: updating subscription ${subscription.id} with calendar ID")
                subscriptionsDao.updateCalendarId(subscription.id, calendar.id)

            } else {
                // before v2.1: if there's no subscription with the same URL
                val newSubscription = Subscription.fromLegacyCalendar(calendar)
                Log.i(TAG, "Migrating from < v2.1: creating subscription $newSubscription")
                val subscriptionId = subscriptionsDao.add(newSubscription)

                // migrate credentials, too (if available)
                val (legacyUsername, legacyPassword) = legacyCredentials.get(calendar)
                if (legacyUsername != null && legacyPassword != null)
                    credentialsDao.create(Credential(subscriptionId, legacyUsername, legacyPassword))
            }

            // set MANAGED_BY_DB=1 so that the calendar won't be migrated anymore
            calendar.setManagedByDB()
        }
    }

    /**
     * Updates the local calendars according to the available [Subscription]s. A local calendar is
     *
     * - created if there's a [Subscription] without calendar,
     * - updated (e.g. display name) if there's a [Subscription] for this calendar,
     * - deleted if there's no [Subscription] for this calendar.
     */
    private suspend fun updateLocalCalendars() {
        // subscriptions from DB
        val subscriptions = subscriptionsDao.getAll()

        // local calendars from provider as Map: <Calendar ID, LocalCalendar>
        val calendars = LocalCalendar.findManaged(account, provider).associateBy { it.id }.toMutableMap()

        // synchronize them
        for ((i, subscription) in subscriptions.withIndex()) {
            updateForegroundInfo(SyncSteps.Subscriptions(subscriptions.size, i, subscription.id))

            val calendarId = subscription.calendarId
            val calendar = calendars.remove(calendarId)
            // note that calendar might still be null even if calendarId is not null,
            // for instance when the calendar has been removed from the system

            if (calendar == null) {
                // no local calendar yet, create it
                Log.d(TAG, "Creating local calendar from subscription #${subscription.id}")
                // create local calendar
                val uri = AndroidCalendar.create(account, provider, subscription.toCalendarProperties())
                // update calendar ID in DB
                val newCalendarId = ContentUris.parseId(uri)
                subscriptionsDao.updateCalendarId(subscription.id, newCalendarId)

            } else {
                // local calendar already existing, update accordingly
                Log.d(TAG, "Updating local calendar #$calendarId from subscription")
                calendar.update(subscription.toCalendarProperties())
            }
        }

        // remove remaining calendars
        for (calendar in calendars.values) {
            Log.d(TAG, "Removing local calendar #${calendar.id} without subscription")
            calendar.delete()
        }
    }

    /**
     * Updates the progress information for the worker. Sets the value of [foregroundInfo] so that
     * it can be fetched by [getForegroundInfo] at any moment.
     *
     * Also updates the current progress of synchronization with [setProgress].
     *
     * @param step The current step of synchronization.
     */
    private suspend fun updateForegroundInfo(step: SyncSteps): ForegroundInfo {
        NotificationUtils.createChannels(applicationContext)

        val notificationId = id.toString().hashCode()

        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)

        val title = applicationContext.getString(R.string.notification_sync_title)
        val message = applicationContext.getString(step.displayName)
        val cancel = applicationContext.getString(R.string.sync_cancel)

        val notification = NotificationCompat.Builder(applicationContext, NotificationUtils.CHANNEL_SYNC_PROGRESS)
            .setTicker(title)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.sync_white)
            .setOngoing(true)
            .setProgress(step.max, step.progress, step.indeterminate)
            .addAction(android.R.drawable.ic_delete, cancel, cancelIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        setProgress(
            step.workData().also {
                Log.i("SyncWorker", "Progress: ${step.id} - ${step.progress} / ${step.max} - ${step.subscriptionId}\nData: $it")
            }
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }.also { foregroundInfo = it }
    }

    sealed class SyncSteps {
        companion object {
            /**
             * Provides a sync step from the data given by a worker. Extracts its [SyncSteps.id],
             * which is used for identifying which step is currently running.
             *
             * @param data The data to extract the step from.
             *
             * @return The instance of [SyncSteps] associated with the data stored at [data], or
             * null if the data doesn't contain a step name, or it could not be recognized.
             */
            fun fromData(data: Data): SyncSteps? = when(data.getString(PROGRESS_STEP)) {
                Start.id -> Start
                Migration.id -> Migration
                Subscriptions.id -> Subscriptions(
                    data.getInt(PROGRESS_MAX, -1),
                    data.getInt(PROGRESS_CURRENT, -1),
                    data.getLong(PROGRESS_SUBSCRIPTION, -1)
                )
                ProcessEvents.id -> ProcessEvents(
                    data.getInt(PROGRESS_MAX, -1),
                    data.getInt(PROGRESS_CURRENT, -1),
                    data.getLong(PROGRESS_SUBSCRIPTION, -1)
                )
                Calendar.id -> Calendar(
                    data.getInt(PROGRESS_MAX, -1),
                    data.getInt(PROGRESS_CURRENT, -1),
                    data.getLong(PROGRESS_SUBSCRIPTION, -1)
                )
                else -> null
            }
        }

        abstract val max: Int
        abstract val progress: Int
        abstract val indeterminate: Boolean

        /** can be used for relating a progress step with a subscription */
        abstract val subscriptionId: Long?

        /** The step name, will be shown in the notification's text */
        @get:StringRes
        abstract val displayName: Int

        /** The identifier of this step. Usually a short name of the object implementing the class */
        abstract val id: String

        /** Obtains a float from 0 to 1 representing the proportion between [progress] and [max] */
        val percentage: Float
            get() = progress.toFloat() / max

        fun workData(): Data = workDataOf(
            PROGRESS_STEP to id,
            PROGRESS_CURRENT to progress,
            PROGRESS_MAX to max,
            PROGRESS_SUBSCRIPTION to subscriptionId
        )

        object Start: SyncSteps() {
            override val max: Int = -1
            override val progress: Int = -1
            override val indeterminate: Boolean = true

            override val subscriptionId: Long? = null

            override val displayName: Int = R.string.notification_sync_start

            override val id: String = "start"
        }

        object Migration: SyncSteps() {
            override val max: Int = -1
            override val progress: Int = -1
            override val indeterminate: Boolean = true

            override val subscriptionId: Long? = null

            override val displayName: Int = R.string.notification_sync_migration

            override val id: String = "migration"
        }

        class Subscriptions(
            override val max: Int,
            override val progress: Int,
            override val subscriptionId: Long
        ): SyncSteps() {
            companion object {
                const val id: String = "subscriptions"
            }

            override val indeterminate: Boolean = false

            override val displayName: Int = R.string.notification_sync_calendar

            override val id: String = Companion.id
        }

        class ProcessEvents(
            override val max: Int,
            override val progress: Int,
            override val subscriptionId: Long
        ): SyncSteps() {
            companion object {
                const val id: String = "process_events"
            }

            override val indeterminate: Boolean = false

            override val displayName: Int = R.string.notification_sync_events

            override val id: String = Companion.id
        }

        class Calendar(
            override val max: Int,
            override val progress: Int,
            override val subscriptionId: Long
        ): SyncSteps() {
            companion object {
                const val id: String = "calendar"
            }

            override val indeterminate: Boolean = false

            override val displayName: Int = R.string.notification_sync_calendar

            override val id: String = Companion.id
        }
    }

}