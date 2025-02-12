package at.bitfire.icsdroid

import android.content.ContentProviderClient
import android.content.ContentUris
import android.content.Context
import android.os.DeadObjectException
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import at.bitfire.ical4android.AndroidCalendar
import at.bitfire.ical4android.util.MiscUtils.closeCompat
import at.bitfire.icsdroid.calendar.LocalCalendar
import at.bitfire.icsdroid.db.AppDatabase
import at.bitfire.icsdroid.db.CalendarCredentials
import at.bitfire.icsdroid.db.entity.Credential
import at.bitfire.icsdroid.db.entity.Subscription
import at.bitfire.icsdroid.ui.NotificationUtils

open class BaseSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    companion object {
        /**
         * An input data (Boolean) for the Worker that tells whether the synchronization should
         * ignore the lastModified timestamp and fetch everything from the server again.
         */
        const val FORCE_RESYNC = "forceResync"

        /**
         * An input data (Boolean) for the Worker that tells if only migration should be performed, without
         * fetching data.
         */
        const val ONLY_MIGRATE = "onlyMigration"
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
        Log.i(Constants.TAG, "Synchronizing (forceReSync=$forceReSync,onlyMigrate=$onlyMigrate)")

        provider = try {
            LocalCalendar.getCalendarProvider(applicationContext)
        } catch (_: SecurityException) {
            NotificationUtils.showCalendarPermissionNotification(applicationContext)
            return Result.failure()
        }

        var syncFailed = false

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
                val success = ProcessEventsTask(
                    applicationContext,
                    subscription,
                    calendar,
                    forceReSync
                ).sync()
                // If the task has failed, set the flag
                if (!success) {
                    Log.e(Constants.TAG, "Task sync not successful")
                    syncFailed = true
                }
            }
        } catch (e: DeadObjectException) {
            /* May happen when the remote process dies or (since Android 14) when IPC (for instance
            with the calendar provider) is suddenly forbidden because our sync process was demoted
            from a "service process" to a "cached process". */
            Log.e(Constants.TAG, "Received DeadObjectException, treating as soft error", e)
            return Result.retry()
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Thread interrupted", e)
            return Result.retry()
        } finally {
            provider.closeCompat()
        }

        return if (syncFailed)
            Result.failure()
        else
            Result.success()
    }

    /**
     * Migrates all the legacy calendar-based subscriptions to the database. Performs these steps:
     *
     * 1. Searches for all the calendars created
     * 2. Checks that those calendars have a matching [Subscription] in the database.
     * 3. If there's no matching [Subscription], create it.
     */
    private suspend fun migrateLegacyCalendars() {
        @Suppress("DEPRECATION")
        val legacyCredentials by lazy { CalendarCredentials(applicationContext) }

        // if there's a provider available, get all the calendars available in the system
        for (calendar in LocalCalendar.findUnmanaged(account, provider)) {
            Log.i(Constants.TAG, "Found unmanaged (<= v2.1.1) calendar ${calendar.id}, migrating")
            @Suppress("DEPRECATION")
            val url = calendar.url ?: continue

            // Special case v2.1: it created subscriptions, but did not set the COLUMN_MANAGED_BY_DB flag.
            val subscription = subscriptionsDao.getByUrl(url)
            if (subscription != null) {
                // So we already have a subscription and only net to set its calendar_id.
                Log.i(
                    Constants.TAG,
                    "Migrating from v2.1: updating subscription ${subscription.id} with calendar ID"
                )
                subscriptionsDao.updateCalendarId(subscription.id, calendar.id)

            } else {
                // before v2.1: if there's no subscription with the same URL
                val newSubscription = Subscription.fromLegacyCalendar(calendar)
                Log.i(
                    Constants.TAG,
                    "Migrating from < v2.1: creating subscription $newSubscription"
                )
                val subscriptionId = subscriptionsDao.add(newSubscription)

                // migrate credentials, too (if available)
                val (legacyUsername, legacyPassword) = legacyCredentials.get(calendar)
                if (legacyUsername != null && legacyPassword != null)
                    credentialsDao.create(
                        Credential(
                            subscriptionId,
                            legacyUsername,
                            legacyPassword
                        )
                    )
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
        val calendars =
            LocalCalendar.findManaged(account, provider).associateBy { it.id }.toMutableMap()

        // synchronize them
        for (subscription in subscriptions) {
            val calendarId = subscription.calendarId
            val calendar = calendars.remove(calendarId)
            // note that calendar might still be null even if calendarId is not null,
            // for instance when the calendar has been removed from the system

            if (calendar == null) {
                // no local calendar yet, create it
                Log.d(
                    Constants.TAG,
                    "Creating local calendar from subscription #${subscription.id}"
                )
                // create local calendar
                val uri = AndroidCalendar.create(account, provider, subscription.toCalendarProperties())
                // update calendar ID in DB
                val newCalendarId = ContentUris.parseId(uri)
                subscriptionsDao.updateCalendarId(subscription.id, newCalendarId)

            } else {
                // local calendar already existing, update accordingly
                Log.d(Constants.TAG, "Updating local calendar #$calendarId from subscription")
                calendar.update(subscription.toCalendarProperties())
            }
        }

        // remove remaining calendars
        for (calendar in calendars.values) {
            Log.d(Constants.TAG, "Removing local calendar #${calendar.id} without subscription")
            calendar.delete()
        }
    }
}