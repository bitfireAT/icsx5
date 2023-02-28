/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.icsdroid

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.util.Log
import androidx.work.*
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.ContentProviderClientHelper.closeCompat
import at.bitfire.icsdroid.Constants.TAG
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.LocalCalendar
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
         * An input data for the Worker that tells whether the synchronization should be performed
         * without taking into account the current network condition.
         */
        private const val FORCE_RESYNC = "forceResync"

        /**
         * An input data for the Worker that tells if only migration should be performed, without
         * fetching data.
         */
        private const val ONLY_MIGRATE = "onlyMigration"

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

    private var forceReSync: Boolean = false

    override suspend fun doWork(): Result {
        forceReSync = inputData.getBoolean(FORCE_RESYNC, false)
        val onlyMigrate = inputData.getBoolean(ONLY_MIGRATE, false)
        Log.i(TAG, "Synchronizing (forceReSync=$forceReSync,onlyMigrate=$onlyMigrate)")

        provider = LocalCalendar.getCalendarProvider(applicationContext)
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
            for (subscription in subscriptionsDao.getAll()) {
                // Make sure the subscription has a matching calendar
                subscription.calendarId ?: continue
                val calendar = LocalCalendar.findById(account, provider, subscription.calendarId)
                ProcessEventsTask(applicationContext, subscription, calendar, forceReSync).sync()
            }
        } catch (e: SecurityException) {
            NotificationUtils.showCalendarPermissionNotification(applicationContext)
            return Result.failure()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Thread interrupted", e)
            return Result.retry()
        } finally {
            provider.closeCompat()
        }

        return Result.success()
    }

    /**
     * Migrates all the legacy calendar-based subscriptions to the database. Performs these steps:
     *
     * 1. Searches for all the calendars created
     * 2. Checks that those calendars have a matching [Subscription] in the database.
     * 3. If there's no matching [Subscription], create it.
     */
    @Suppress("DEPRECATION")
    private fun migrateLegacyCalendars() {
        val legacyCredentials = CalendarCredentials(applicationContext)

        // if there's a provider available, get all the calendars available in the system
        for (calendar in LocalCalendar.findAll(account, provider)) {
            val match = subscriptionsDao.getByCalendarId(calendar.id)
            if (match == null) {
                // still no subscription for this calendar ID, create one (= migration)
                val newSubscription = Subscription.fromLegacyCalendar(calendar)
                subscriptionsDao.add(newSubscription)
                Log.i(TAG, "The calendar #${calendar.id} didn't have a matching subscription. Just created it.")

                // migrate credentials, too (if available)
                val (legacyUsername, legacyPassword) = legacyCredentials.get(calendar)
                if (legacyUsername != null && legacyPassword != null) {
                    // Subscription ID has been assigned automatically, so fetch it
                    val id = subscriptionsDao.getByCalendarId(calendar.id)?.id ?: continue
                    credentialsDao.create(Credential(
                        id, legacyUsername, legacyPassword
                    ))
                }
            }
        }
    }

    /**
     * Updates the local calendars according to the available [Subscription]s. A local calendar is
     *
     * - created if there's a [Subscription] without calendar,
     * - updated (e.g. display name) if there's a [Subscription] for this calendar,
     * - deleted if there's no [Subscription] for this calendar.
     */
    private fun updateLocalCalendars() {
        val subscriptions = subscriptionsDao.getAll()
        val calendars = LocalCalendar.findAll(account, provider).associateBy { it.id }.toMutableMap()

        for (subscription in subscriptions) {
            val calendar = subscription.calendarId?.let { calendars.remove(it) }
            if (calendar != null) {
                Log.d(TAG, "Updating local calendar #${calendar.id} from subscription")
                calendar.update(subscription.toCalendarProperties())
            } else {
                Log.d(TAG, "Creating local calendar from subscription #${subscription.id}")
                val uri = AndroidCalendar.create(account, provider, subscription.toCalendarProperties())
                val calendarId = ContentUris.parseId(uri)
                subscriptionsDao.updateCalendarId(subscription.id, calendarId)
            }
        }

        // remove remaining calendars
        for (calendar in calendars.values) {
            Log.d(TAG, "Removing local calendar #${calendar.id} without subscription")
            calendar.delete()
        }
    }

}